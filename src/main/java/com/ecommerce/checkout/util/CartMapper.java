package com.ecommerce.checkout.util;

import com.ecommerce.checkout.dto.CartSnapshotDto;
import com.ecommerce.checkout.model.CartDocument;

import java.util.List;

public final class CartMapper {
    private CartMapper() {
    }

    public static CartSnapshotDto toDto(CartDocument cartDocument) {
        var lines = cartDocument.lines == null ? List.<CartSnapshotDto.Line>of() :
                cartDocument.lines.stream().map(line -> new CartSnapshotDto.Line(
                        line.lineId, line.sku, line.productId, line.qty,
                        new CartSnapshotDto.PriceBreakdown(
                                nz(line.price.unitPriceCents),
                                nz(line.price.unitPromoAdjCents),
                                nz(line.price.unitTaxCents),
                                nz(line.price.lineBaseCents),
                                nz(line.price.linePromoAdjCents),
                                nz(line.price.lineTaxCents),
                                nz(line.price.lineTotalCents)
                        ),
                        new CartSnapshotDto.AvailabilityAdvisory(
                                line.availability.inStock,
                                line.availability.backorder,
                                line.availability.availableQty,
                                line.availability.etaText
                        ),
                        line.attributes,
                        line.metadata
                )).toList();
        return new CartSnapshotDto(
                cartDocument.id,
                cartDocument.version,
                cartDocument.currency,
                cartDocument.country,
                nz(cartDocument.subtotalCents),
                nz(cartDocument.promoDiscountCents),
                nz(cartDocument.taxCents),
                nz(cartDocument.shippingCents),
                nz(cartDocument.totalCents),
                lines,
                cartDocument.warnings,
                cartDocument.updatedAt
        );


    }

    private static Long nz(Long v){ return v == null ? 0L : v; }
}
