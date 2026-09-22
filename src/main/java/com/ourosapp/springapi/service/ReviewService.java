package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.FarmTip;
import com.ourosapp.springapi.entity.Review;
import com.ourosapp.springapi.entity.Tip;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.FarmTipRepository;
import com.ourosapp.springapi.repository.ReviewRepository;
import com.ourosapp.springapi.repository.TipRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço responsável pelas regras de negócio e operações de persistência da entidade {@link Review} (Avaliação de Dica Técnica).
 * Implementa validações de segurança, isolamento por tenant (multi-tenancy) e integridade de dados.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TipRepository tipRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final FarmTipRepository farmTipRepository;

    /**
     * Cadastra uma nova avaliação/review para uma dica técnica específica.
     *
     * @param tipId     identificador único da dica técnica a ser avaliada
     * @param request   payload com o comentário e nota da avaliação
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados da avaliação cadastrada
     */
    @Transactional
    public ReviewResponseDTO createReview(Long tipId, ReviewRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        checkAuthenticated(principal);

        Tip tip = findTipByIdOrThrow(tipId);
        validateTipAccess(tip, principal, "avaliar");

        if (request.rating() != null && (request.rating() < 0 || request.rating() > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nota da avaliação deve estar entre 0 e 5"
            );
        }

        Review review = Review.builder()
                .comment(request.comment())
                .rating(request.rating())
                .idTip(tip.getId())
                .build();

        Review saved = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(saved);
    }

    /**
     * Lista todas as avaliações registradas para uma dica técnica específica.
     *
     * @param tipId     identificador único da dica técnica
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs com as avaliações da dica
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByTipId(Long tipId, UserPrincipal principal) {
        checkAuthenticated(principal);

        Tip tip = findTipByIdOrThrow(tipId);
        validateTipAccess(tip, principal, "consultar avaliações de");

        return reviewRepository.findByIdTip(tip.getId())
                .stream()
                .map(ReviewResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca os detalhes de uma avaliação específica pelo seu ID.
     *
     * @param id        identificador único da avaliação
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os detalhes da avaliação
     */
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewById(Long id, UserPrincipal principal) {
        checkAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "consultar");

        return ReviewResponseDTO.fromEntity(review);
    }

    /**
     * Atualiza parcialmente uma avaliação existente (PATCH /reviews/{id}).
     *
     * @param id        identificador único da avaliação a ser atualizada
     * @param request   payload com os campos parciais
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com a avaliação atualizada
     */
    @Transactional
    public ReviewResponseDTO updateReview(Long id, ReviewUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        checkAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "alterar esta avaliação");

        if (!request.hasUpdates()) {
            return ReviewResponseDTO.fromEntity(review);
        }

        if (request.rating() != null && (request.rating() < 0 || request.rating() > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nota da avaliação deve estar entre 0 e 5"
            );
        }

        if (request.comment() != null && !request.comment().isBlank()) {
            review.setComment(request.comment());
        }
        if (request.rating() != null) {
            review.setRating(request.rating());
        }

        Review updated = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(updated);
    }

    /**
     * Remove uma avaliação do sistema (DELETE /reviews/{id}).
     *
     * @param id        identificador único da avaliação a ser removida
     * @param principal dados do usuário logado extraídos do token JWT
     */
    @Transactional
    public void deleteReview(Long id, UserPrincipal principal) {
        checkAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "remover esta avaliação");

        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    private void checkAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    /**
     * Valida permissões de acesso do perfil com base no vínculo da fazenda da dica.
     *
     * @param tip       dica técnica associada à avaliação
     * @param principal dados do usuário logado
     * @param action    descrição textual da ação
     */
    private void validateTipAccess(Tip tip, UserPrincipal principal, String action) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Funcionário logado não encontrado para o ID: " + principal.getId()
                    ));
            Farm primaryFarm = farmRepository.findById(tip.getIdFarm())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Fazenda não encontrada para o ID: " + tip.getIdFarm()
                    ));

            boolean belongsToEnterprise = Objects.equals(primaryFarm.getIdEnterprise(), employee.getIdEnterprise())
                    || farmTipRepository.findByIdTip(tip.getId()).stream()
                            .map(FarmTip::getIdFarm)
                            .map(farmRepository::findById)
                            .flatMap(Optional::stream)
                            .anyMatch(f -> Objects.equals(f.getIdEnterprise(), employee.getIdEnterprise()));

            if (!belongsToEnterprise) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra empresa"
                );
            }
            return;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Produtor rural logado não encontrado para o ID: " + principal.getId()
                    ));

            boolean belongsToFarm = Objects.equals(tip.getIdFarm(), owner.getIdFarm())
                    || (owner.getIdFarm() != null && farmTipRepository.existsByIdFarmAndIdTip(owner.getIdFarm(), tip.getId()));

            if (!belongsToFarm) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra fazenda"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " avaliações"
        );
    }

    private Tip findTipByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dica técnica não encontrada para o ID: null");
        }
        return tipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dica técnica não encontrada para o ID: " + id
                ));
    }

    private Review findReviewByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada para o ID: null");
        }
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Avaliação não encontrada para o ID: " + id
                ));
    }
}
