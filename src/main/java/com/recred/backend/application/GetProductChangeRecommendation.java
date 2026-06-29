package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GetProductChangeRecommendation {

    private final KiosqueroRepository kiosqueroRepository;
    private final AIEventRepository aiEventRepository;

    public GetProductChangeRecommendation(
            KiosqueroRepository kiosqueroRepository,
            AIEventRepository aiEventRepository
    ) {
        this.kiosqueroRepository = kiosqueroRepository;
        this.aiEventRepository = aiEventRepository;
    }

    @Transactional(readOnly = true)
    public List<GetProductChangeRecommendationResponse> execute(
            GetProductChangeRecommendationRequest request) {
        UUID buffetId = findBuffetId(request.usuarioId());
        List<ProductRecommendationResult> results = findRecommendations(buffetId);
        return mapToResponse(results);
    }

    private UUID findBuffetId(UUID usuarioId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(usuarioId);
        }
        return buffetId;
    }

    private List<ProductRecommendationResult> findRecommendations(UUID buffetId) {
        return aiEventRepository.findAllProductChangeRecommendations(buffetId);
    }

    private List<GetProductChangeRecommendationResponse> mapToResponse(
            List<ProductRecommendationResult> results) {
        return results.stream()
                .map(this::mapItemToResponse)
                .toList();
    }

    private GetProductChangeRecommendationResponse mapItemToResponse(
            ProductRecommendationResult result) {
        ProductChangeRecommendation recommendation = result.getProductChangeRecommendation();
        ProductChangeAnalysis analysis = result.getAiAnalysis().orElse(null);
        ProductSaleStatistics stats = recommendation.getSaleStatistics();

        return new GetProductChangeRecommendationResponse(
                recommendation.getId(),
                recommendation.getNombre(),
                recommendation.getPrecio(),
                analysis != null ? analysis.summary() : null,
                analysis != null ? analysis.alerts() : List.of(),
                analysis != null ? analysis.recommendations() : List.of(),
                analysis != null ? analysis.model() : null,
                mapStatsToDto(stats)
        );
    }

    private GetProductChangeRecommendationResponse.ProductSaleStatisticsDto mapStatsToDto(
            ProductSaleStatistics stats) {
        if (stats == null) {
            return null;
        }
        return new GetProductChangeRecommendationResponse.ProductSaleStatisticsDto(
                stats.getProductoId(),
                stats.getNombre(),
                stats.getCategoria(),
                stats.getPrecioActual(),
                stats.getVentasPeriodo(),
                stats.getParticipacionVentas(),
                stats.getRankingGeneral(),
                stats.getRankingCategoria(),
                stats.getPromedioVentasCategoria(),
                stats.getPromedioPrecioCategoria(),
                stats.getDiferenciaPrecioCategoria(),
                stats.getDiasSinVenta(),
                stats.getClientesDistintos(),
                stats.getStockActual(),
                stats.getStockPromedioCategoria()
        );
    }
}
