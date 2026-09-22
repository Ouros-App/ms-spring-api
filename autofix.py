import subprocess

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
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """        try {
            Review saved = reviewRepository.save(review);
            return ReviewResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar avaliação",
                    ex
            );
        }""",
        """        Review saved = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(saved);"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """        try {
            Review updated = reviewRepository.save(review);
            return ReviewResponseDTO.fromEntity(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar avaliação",
                    ex
            );
        }""",
        """        Review updated = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(updated);"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """        try {
            reviewRepository.delete(review);
            reviewRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover a avaliação pois existem outros dados vinculados a ela",
                    ex
            );
        }""",
        """        reviewRepository.delete(review);
        reviewRepository.flush();"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/TipService.java",
        """        try {
            Tip saved = tipRepository.save(tip);

            if (!farmTipRepository.existsByIdFarmAndIdTip(farm.getId(), saved.getId())) {
                farmTipRepository.save(FarmTip.builder()
                        .idFarm(farm.getId())
                        .idTip(saved.getId())
                        .build());
            }

            if (!categories.isEmpty()) {
                for (Category cat : categories) {
                    if (!tipCategoryRepository.existsByIdTipAndIdCategory(saved.getId(), cat.getId())) {
                        tipCategoryRepository.save(TipCategory.builder()
                                .idTip(saved.getId())
                                .idCategory(cat.getId())
                                .build());
                    }
                }
            }

            List<String> categoryNames = categories.stream()
                    .map(Category::getCategory)
                    .distinct()
                    .toList();

            return TipResponseDTO.fromEntity(saved, categoryNames, 0, 0.0);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar dica técnica",
                    ex
            );
        }""",
        """        Tip saved = tipRepository.save(tip);

        if (!farmTipRepository.existsByIdFarmAndIdTip(farm.getId(), saved.getId())) {
            farmTipRepository.save(FarmTip.builder()
                    .idFarm(farm.getId())
                    .idTip(saved.getId())
                    .build());
        }

        if (!categories.isEmpty()) {
            for (Category cat : categories) {
                if (!tipCategoryRepository.existsByIdTipAndIdCategory(saved.getId(), cat.getId())) {
                    tipCategoryRepository.save(TipCategory.builder()
                            .idTip(saved.getId())
                            .idCategory(cat.getId())
                            .build());
                }
            }
        }

        List<String> categoryNames = categories.stream()
                .map(Category::getCategory)
                .distinct()
                .toList();

        return TipResponseDTO.fromEntity(saved, categoryNames, 0, 0.0);"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/TipService.java",
        """        try {
            Tip updated = tipRepository.save(tip);
            return enrichTips(List.of(updated)).get(0);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar dica técnica",
                    ex
            );
        }""",
        """        Tip updated = tipRepository.save(tip);
        return enrichTips(List.of(updated)).get(0);"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/TipService.java",
        """        try {
            tipCategoryRepository.deleteByIdTip(tip.getId());
            farmTipRepository.deleteByIdTip(tip.getId());
            reviewRepository.deleteByIdTip(tip.getId());
            tipRepository.delete(tip);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover a dica técnica pois existem dados vinculados a ela",
                    ex
            );
        }""",
        """        tipCategoryRepository.deleteByIdTip(tip.getId());
        farmTipRepository.deleteByIdTip(tip.getId());
        reviewRepository.deleteByIdTip(tip.getId());
        tipRepository.delete(tip);"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/CategoryService.java",
        """        try {
            Category saved = categoryRepository.save(category);

            if (!tipCategoryRepository.existsByIdTipAndIdCategory(tip.getId(), saved.getId())) {
                tipCategoryRepository.save(TipCategory.builder()
                        .idTip(tip.getId())
                        .idCategory(saved.getId())
                        .build());
            }

            return CategoryResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar categoria",
                    ex
            );
        }""",
        """        Category saved = categoryRepository.save(category);

        if (!tipCategoryRepository.existsByIdTipAndIdCategory(tip.getId(), saved.getId())) {
            tipCategoryRepository.save(TipCategory.builder()
                    .idTip(tip.getId())
                    .idCategory(saved.getId())
                    .build());
        }

        return CategoryResponseDTO.fromEntity(saved);"""
    )

if __name__ == "__main__":
    main()
