package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanProduct entities.
 */
public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {

    /**
     * Find loan product by product code.
     *
     * @param productCode the unique product code
     * @return optional containing the product if found
     */
    Optional<LoanProduct> findByProductCode(String productCode);

    /**
     * Find loan product by name.
     *
     * @param productName the product name
     * @return optional containing the product if found
     */
    Optional<LoanProduct> findByProductName(String productName);

    /**
     * Find loan products by type.
     *
     * @param productType the product type
     * @param pageable pagination information
     * @return page of loan products of the specified type
     */
    Page<LoanProduct> findByProductType(LoanProductType productType, Pageable pageable);

    /**
     * Find active loan products.
     *
     * @param pageable pagination information
     * @return page of active loan products
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.active = true
            ORDER BY lp.productName ASC
            """)
    Page<LoanProduct> findActiveProducts(Pageable pageable);

    /**
     * Find active loan products (non-paginated).
     *
     * @return list of active loan products
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.active = true
            ORDER BY lp.productName ASC
            """)
    List<LoanProduct> findActiveProducts();

    /**
     * Find loan products by currency.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of loan products in the specified currency
     */
    Page<LoanProduct> findByCurrency(String currency, Pageable pageable);

    /**
     * Find loan products with interest rate in range.
     *
     * @param minRate minimum interest rate
     * @param maxRate maximum interest rate
     * @param pageable pagination information
     * @return page of loan products in the rate range
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.interestRate >= :minRate
            AND lp.interestRate <= :maxRate
            AND lp.active = true
            ORDER BY lp.interestRate ASC
            """)
    Page<LoanProduct> findByInterestRateRange(@Param("minRate") BigDecimal minRate,
            @Param("maxRate") BigDecimal maxRate, Pageable pageable);

    /**
     * Find loan products with amount in range.
     *
     * @param minAmount minimum loan amount
     * @param maxAmount maximum loan amount
     * @param pageable pagination information
     * @return page of loan products in the amount range
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.minAmount <= :maxAmount
            AND lp.maxAmount >= :minAmount
            AND lp.active = true
            ORDER BY lp.minAmount ASC
            """)
    Page<LoanProduct> findByLoanAmountRange(@Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);

    /**
     * Find loan products requiring collateral.
     *
     * @param pageable pagination information
     * @return page of loan products requiring collateral
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.collateralRequired = true
            AND lp.active = true
            """)
    Page<LoanProduct> findProductsRequiringCollateral(Pageable pageable);

    /**
     * Find loan products requiring guarantors.
     *
     * @param pageable pagination information
     * @return page of loan products requiring guarantors
     */
    @Query("""
            SELECT lp FROM LoanProduct lp
            WHERE lp.guarantorRequired = true
            AND lp.active = true
            """)
    Page<LoanProduct> findProductsRequiringGuarantors(Pageable pageable);

    /**
     * Count active loan products.
     *
     * @return count of active products
     */
    @Query("""
            SELECT COUNT(lp) FROM LoanProduct lp
            WHERE lp.active = true
            """)
    long countActiveProducts();

    /**
     * Count loan products by type.
     *
     * @param productType the product type
     * @return count of products of the specified type
     */
    long countByProductType(LoanProductType productType);
}
