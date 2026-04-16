package dev.manalith.catalog.dto;

import java.util.List;

/**
 * Paginated search result returned by {@code GET /api/catalog/cards/search}.
 *
 * @param cards    the matching card printings for this page
 * @param total    total number of results across all pages
 * @param page     zero-based current page index
 * @param pageSize number of items per page
 * @param hasMore  {@code true} if there are more pages after this one
 */
public record CardSearchResultDTO(
        List<CardPrintingDTO> cards,
        int total,
        int page,
        int pageSize,
        boolean hasMore
) {}
