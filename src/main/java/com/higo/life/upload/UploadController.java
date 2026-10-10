package com.higo.life.upload;
import com.higo.life.auth.CurrentUser;
import com.higo.life.support.*;
import jakarta.annotation.PostConstruct;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/uploads")
public class UploadController {
    private final Path root; private final CurrentUser current;
    public UploadController(@Value("${higo.upload.dir:./data/uploads}") String root,CurrentUser current) { this.root=Path.of(root).toAbsolutePath().normalize();this.current=current; }
    @PostConstruct void init() throws IOException { Files.createDirectories(root); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long user=current.require().id();
        if(file.isEmpty() || file.getSize()>5*1024*1024) throw new InvalidRequestException("图片大小必须在 1 字节到 5MB 之间");
        byte[] bytes=file.getBytes();
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if(stream==null) throw new InvalidRequestException("图片格式不支持");
            var readers=ImageIO.getImageReaders(stream); if(!readers.hasNext()) throw new InvalidRequestException("仅支持 PNG/JPEG/GIF 图片");
            var reader=readers.next();
            try {
                reader.setInput(stream); String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpeg","jpg","gif").contains(format)) throw new InvalidRequestException("仅支持 PNG/JPEG/GIF 图片");
                if((long)reader.getWidth(0)*reader.getHeight(0)>20_000_000) throw new InvalidRequestException("图片像素超过限制");
                reader.read(0);
                String name=Long.toHexString(user)+"-"+UUID.randomUUID()+"."+(format.equals("jpeg")?"jpg":format);
                Files.write(root.resolve(name),bytes,StandardOpenOption.CREATE_NEW);
                return Map.of("name",name,"url","/api/uploads/"+name);
            } finally { reader.dispose(); }
        }
    }
    private Path path(String name) {
        if(!name.matches("[a-f0-9]+-[a-f0-9-]{36}\\.(png|jpg|gif)")) throw new NotFoundException("图片不存在");
        Path p=root.resolve(name).normalize(); if(!p.getParent().equals(root)) throw new NotFoundException("图片不存在"); return p;
    }
    @GetMapping("/{name}") public ResponseEntity<byte[]> read(@PathVariable String name) throws IOException {
        Path p=path(name); if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)) throw new NotFoundException("图片不存在");
        MediaType type=name.endsWith(".png")?MediaType.IMAGE_PNG:name.endsWith(".gif")?MediaType.IMAGE_GIF:MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options","nosniff").body(Files.readAllBytes(p));
    }
    @DeleteMapping("/{name}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String name) throws IOException {
        Path p=path(name); if(!name.startsWith(Long.toHexString(current.require().id())+"-")) throw new NotFoundException("图片不存在"); Files.deleteIfExists(p);
    }
}
