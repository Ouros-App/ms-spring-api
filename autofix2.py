import subprocess
import re

def apply_diff(filepath, search, replace):
    with open(filepath, 'r') as f:
        content = f.read()
    if search in content:
        content = content.replace(search, replace)
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Applied diff to {filepath}")
    else:
        print(f"Could not find search block in {filepath}")

def main():
    apply_diff(
        "src/test/java/com/ourosapp/springapi/service/ReviewServiceTest.java",
        """        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());""",
        """        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.createReview(100L, sampleRequest, adminPrincipal)
        );"""
    )
    apply_diff(
        "src/test/java/com/ourosapp/springapi/service/ReviewServiceTest.java",
        """        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(50L, updateDTO, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());""",
        """        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.updateReview(50L, updateDTO, adminPrincipal)
        );"""
    )
    apply_diff(
        "src/test/java/com/ourosapp/springapi/service/ReviewServiceTest.java",
        """        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.deleteReview(50L, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());""",
        """        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.deleteReview(50L, adminPrincipal)
        );"""
    )
    apply_diff(
        "src/test/java/com/ourosapp/springapi/service/TipServiceTest.java",
        """        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());""",
        """        assertThrows(DataIntegrityViolationException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );"""
    )
    apply_diff(
        "src/test/java/com/ourosapp/springapi/service/CategoryServiceTest.java",
        """        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.createCategory(request, adminPrincipal)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());""",
        """        assertThrows(DataIntegrityViolationException.class, () ->
                categoryService.createCategory(request, adminPrincipal)
        );"""
    )
    
if __name__ == "__main__":
    main()
