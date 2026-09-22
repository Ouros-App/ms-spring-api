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
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """    private final FarmOwnerRepository farmOwnerRepository;""",
        """    private final FarmOwnerRepository farmOwnerRepository;
    private final com.ourosapp.springapi.repository.FarmTipRepository farmTipRepository;"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(tip.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra fazenda"
                );
            }
            return;
        }""",
        """        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(tip.getIdFarm(), owner.getIdFarm()) && 
                (owner.getIdFarm() == null || !farmTipRepository.existsByIdFarmAndIdTip(owner.getIdFarm(), tip.getId()))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra fazenda"
                );
            }
            return;
        }"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/ReviewService.java",
        """        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(tip.getIdFarm());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra empresa"
                );
            }
            return;
        }""",
        """        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm primaryFarm = findFarmByIdOrThrow(tip.getIdFarm());
            boolean isPrimaryFarmOfEnterprise = Objects.equals(primaryFarm.getIdEnterprise(), employee.getIdEnterprise());
            boolean isSharedWithEnterprise = farmTipRepository.findByIdTip(tip.getId()).stream()
                    .map(ft -> findFarmByIdOrThrow(ft.getIdFarm()))
                    .anyMatch(f -> Objects.equals(f.getIdEnterprise(), employee.getIdEnterprise()));

            if (!isPrimaryFarmOfEnterprise && !isSharedWithEnterprise) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra empresa"
                );
            }
            return;
        }"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/service/TipService.java",
        """            case COMPANY_EMPLOYEE -> Objects.equals(
                    primaryFarm.getIdEnterprise(),
                    getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise()
            );""",
        """            case COMPANY_EMPLOYEE -> {
                Long idEnterprise = getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise();
                boolean isPrimaryFarmOfEnterprise = Objects.equals(primaryFarm.getIdEnterprise(), idEnterprise);
                boolean isSharedWithEnterprise = farmTipRepository.findByIdTip(tip.getId()).stream()
                        .map(ft -> findFarmByIdOrThrow(ft.getIdFarm()))
                        .anyMatch(f -> Objects.equals(f.getIdEnterprise(), idEnterprise));
                yield isPrimaryFarmOfEnterprise || isSharedWithEnterprise;
            }"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/entity/Category.java",
        """    @Column(name = "id_tip", nullable = false)
    private Long idTip;
}""",
        """}"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/controller/CategoryController.java",
        """        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();""",
        """        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();"""
    )
    apply_diff(
        "src/main/java/com/ourosapp/springapi/controller/CategoryController.java",
        """        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();""",
        """        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();"""
    )

if __name__ == "__main__":
    main()
