#!/usr/bin/env node

'use strict';

const assert = require('node:assert/strict');

const baseUrls = (process.env.HIGO_BASE_URLS || 'http://127.0.0.1:8080')
  .split(',')
  .map(value => value.trim().replace(/\/$/, ''))
  .filter(Boolean);
const buyerCount = numberOption('HIGO_BUYERS', 24, 2, 200);
const stock = numberOption('HIGO_STOCK', 5, 1, buyerCount - 1);
const completionTimeoutMs = numberOption('HIGO_COMPLETION_TIMEOUT_MS', 60000, 1000, 300000);
const requestTimeoutMs = numberOption('HIGO_REQUEST_TIMEOUT_MS', 10000, 1000, 60000);
const runSeed = Number(String(Date.now()).slice(-5));
let phoneSequence = 0;

function numberOption(name, fallback, minimum, maximum) {
  const value = Number(process.env[name] || fallback);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} must be an integer between ${minimum} and ${maximum}`);
  }
  return value;
}

function nextPhone() {
  const tail = String((runSeed * 10000 + phoneSequence++) % 1000000000).padStart(9, '0');
  return `13${tail}`;
}

function baseAt(index) {
  return baseUrls[index % baseUrls.length];
}

async function request(baseUrl, path, options = {}) {
  const headers = {...(options.headers || {})};
  if (options.token) {
    headers.authorization = `Bearer ${options.token}`;
  }
  let body;
  if (options.body !== undefined) {
    headers['content-type'] = 'application/json';
    body = JSON.stringify(options.body);
  }
  const response = await fetch(baseUrl + path, {
    method: options.method || 'GET',
    headers,
    body,
    signal: AbortSignal.timeout(requestTimeoutMs)
  });
  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }
  return {status: response.status, payload};
}

async function expect(baseUrl, path, status, options = {}) {
  const result = await request(baseUrl, path, options);
  assert.equal(
    result.status,
    status,
    `${options.method || 'GET'} ${baseUrl}${path} returned ${result.status}: ${JSON.stringify(result.payload)}`
  );
  return result.payload;
}

async function login(baseUrl) {
  const phone = nextPhone();
  const codeResponse = await expect(baseUrl, '/api/auth/codes', 200, {
    method: 'POST',
    body: {phone}
  });
  assert.match(
    codeResponse.code || '',
    /^\d{6}$/,
    'The load check requires the default learning mode with higo.auth.expose-code=true'
  );
  const session = await expect(baseUrl, '/api/auth/sessions', 200, {
    method: 'POST',
    body: {phone, code: codeResponse.code}
  });
  return {phone, token: session.token, userId: session.user.id};
}

async function createVoucher(manager, shopId, voucherStock, label) {
  const voucher = await expect(baseUrls[0], '/api/vouchers', 201, {
    method: 'POST',
    token: manager.token,
    body: {
      shopId,
      title: `${label} ${Date.now()}`,
      description: 'multi-instance load verification',
      price: 9.90,
      stock: voucherStock,
      beginAt: '2020-01-01T00:00:00',
      endAt: '2099-01-01T00:00:00'
    }
  });
  return voucher.id;
}

async function reserve(baseUrl, token, voucherId) {
  const result = await request(baseUrl, `/api/seckill/vouchers/${voucherId}/orders`, {
    method: 'POST',
    token
  });
  assert.ok(
    result.status === 202 || result.status === 409,
    `unexpected seckill status ${result.status}: ${JSON.stringify(result.payload)}`
  );
  return {...result, baseUrl, token};
}

async function waitForCompletion(accepted) {
  const deadline = Date.now() + completionTimeoutMs;
  while (Date.now() < deadline) {
    const states = await Promise.all(accepted.map((item, index) =>
      expect(baseAt(index + 1), `/api/seckill/requests/${item.payload.requestId}`, 200, {
        token: item.token
      })
    ));
    const failed = states.find(state => state.status === 'FAILED');
    assert.equal(failed, undefined, `request failed: ${JSON.stringify(failed)}`);
    if (states.every(state => state.status === 'COMPLETED')) {
      return states;
    }
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  throw new Error(`orders did not complete within ${completionTimeoutMs} ms`);
}

async function main() {
  assert.ok(baseUrls.length > 0, 'HIGO_BASE_URLS must contain at least one URL');
  await Promise.all(baseUrls.map(url => expect(url, '/api/shops', 200)));
  console.log(`instances=${baseUrls.join(',')} buyers=${buyerCount} stock=${stock}`);

  const manager = await login(baseUrls[0]);
  const shop = await expect(baseUrls[0], '/api/shops', 201, {
    method: 'POST',
    token: manager.token,
    body: {
      name: `多实例压测店 ${Date.now()}`,
      category: '压测学习',
      address: '仅用于本地学习验证',
      longitude: 113.317,
      latitude: 23.083
    }
  });

  const duplicateBuyer = await login(baseAt(1));
  const duplicateVoucherId = await createVoucher(manager, shop.id, 2, '一人一单验证券');
  const duplicateResults = await Promise.all([
    reserve(baseAt(0), duplicateBuyer.token, duplicateVoucherId),
    reserve(baseAt(1), duplicateBuyer.token, duplicateVoucherId)
  ]);
  const duplicateAccepted = duplicateResults.filter(result => result.status === 202);
  assert.equal(duplicateAccepted.length, 1, 'the same user must reserve the same voucher exactly once');
  await waitForCompletion(duplicateAccepted);
  const duplicateOrders = await expect(baseAt(1), '/api/orders/mine', 200, {
    token: duplicateBuyer.token
  });
  assert.equal(
    duplicateOrders.filter(order => order.voucherId === duplicateVoucherId).length,
    1,
    'the same user received duplicate orders'
  );

  const voucherId = await createVoucher(manager, shop.id, stock, '并发库存验证券');
  const buyers = await Promise.all(Array.from({length: buyerCount}, (_, index) => login(baseAt(index))));
  const results = await Promise.all(buyers.map((buyer, index) =>
    reserve(baseAt(index), buyer.token, voucherId)
  ));
  const accepted = results.filter(result => result.status === 202);
  const rejected = results.filter(result => result.status === 409);
  assert.equal(accepted.length, stock, 'accepted reservations must equal voucher stock');
  assert.equal(rejected.length, buyerCount - stock, 'all excess buyers must be rejected');

  const states = await waitForCompletion(accepted);
  const orderIds = states.map(state => state.orderId);
  assert.equal(new Set(orderIds).size, stock, 'completed requests must map to unique orders');
  assert.ok(orderIds.every(Boolean), 'every completed request must expose its order id');

  const acceptedOrders = await Promise.all(accepted.map((item, index) =>
    expect(baseAt(index + 1), '/api/orders/mine', 200, {token: item.token})
  ));
  assert.ok(
    acceptedOrders.every(orders => orders.filter(order => order.voucherId === voucherId).length === 1),
    'each accepted buyer must own exactly one order'
  );

  console.log(JSON.stringify({
    result: 'PASS',
    instances: baseUrls.length,
    buyers: buyerCount,
    stock,
    accepted: accepted.length,
    rejected: rejected.length,
    duplicateUserAccepted: duplicateAccepted.length,
    uniqueOrders: new Set(orderIds).size
  }, null, 2));
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
