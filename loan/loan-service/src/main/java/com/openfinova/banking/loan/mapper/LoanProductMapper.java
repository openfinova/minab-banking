package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanProductRequest;
import com.openfinova.banking.loan.api.dto.LoanProductResponse;
import com.openfinova.banking.loan.entity.LoanProduct;

/**
 * Mapper for converting between LoanProduct entities and DTOs.
 */
public class LoanProductMapper {

    private LoanProductMapper() {
        // Utility class
    }

    /**
     * Converts a LoanProduct entity to a response DTO.
     */
    public static LoanProductResponse toResponse(LoanProduct product) {
        if (product == null) {
            return null;
        }

        LoanProductResponse response = new LoanProductResponse();
        response.setId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getProductName());
        response.setProductType(product.getProductType());
        response.setDescription(product.getDescription());
        response.setMinAmount(product.getMinAmount());
        response.setMaxAmount(product.getMaxAmount());
        response.setMinTenorMonths(product.getMinTenorMonths());
        response.setMaxTenorMonths(product.getMaxTenorMonths());
        response.setInterestRate(product.getInterestRate());
        response.setInterestCalculationMethod(product.getInterestCalculationMethod());
        response.setRepaymentFrequency(product.getRepaymentFrequency());
        response.setAmortizationType(product.getAmortizationType());
        response.setCurrency(product.getCurrency());
        response.setCollateralRequired(product.getCollateralRequired());
        response.setGuarantorRequired(product.getGuarantorRequired());
        response.setGracePeriodDays(product.getGracePeriodDays());
        response.setProcessingFeePercentage(product.getProcessingFeePercentage());
        response.setProcessingFeeFixed(product.getProcessingFeeFixed());
        response.setLateFeePercentage(product.getLateFeePercentage());
        response.setLateFeeFixed(product.getLateFeeFixed());
        response.setPrepaymentPenaltyPercentage(product.getPrepaymentPenaltyPercentage());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        return response;
    }

    /**
     * Converts a request DTO to a LoanProduct entity.
     */
    public static LoanProduct toEntity(LoanProductRequest request) {
        if (request == null) {
            return null;
        }

        LoanProduct product = new LoanProduct();
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setProductType(request.getProductType());
        product.setDescription(request.getDescription());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setMinTenorMonths(request.getMinTenorMonths());
        product.setMaxTenorMonths(request.getMaxTenorMonths());
        product.setInterestRate(request.getInterestRate());
        product.setInterestCalculationMethod(request.getInterestCalculationMethod());
        product.setRepaymentFrequency(request.getRepaymentFrequency());
        product.setAmortizationType(request.getAmortizationType());
        product.setCurrency(request.getCurrency());
        product.setCollateralRequired(request.getCollateralRequired());
        product.setGuarantorRequired(request.getGuarantorRequired());
        product.setGracePeriodDays(request.getGracePeriodDays());
        product.setProcessingFeePercentage(request.getProcessingFeePercentage());
        product.setProcessingFeeFixed(request.getProcessingFeeFixed());
        product.setLateFeePercentage(request.getLateFeePercentage());
        product.setLateFeeFixed(request.getLateFeeFixed());
        product.setPrepaymentPenaltyPercentage(request.getPrepaymentPenaltyPercentage());

        return product;
    }
}
