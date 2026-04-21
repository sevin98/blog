package sevin.dev.blog.domain.post;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sevin.dev.blog.common.response.ApiResponse;
import sevin.dev.blog.domain.post.dto.CreatePostRequest;
import sevin.dev.blog.domain.post.dto.PostResponse;
import sevin.dev.blog.domain.post.dto.UpdatePostRequest;

@RestController
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(@RequestBody @Valid CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(postService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> findAll(
            @RequestParam(required = false) PostStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(postService.findAll(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(postService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
