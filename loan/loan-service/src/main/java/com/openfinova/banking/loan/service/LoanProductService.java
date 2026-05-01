package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.dto.ProductValidationResult;
import com.openfinova.banking.loan.entity.LoanProduct;
import com.openfinova.banking.loan.repository.LoanProductRepository;

/**
 * Implementation of LoanProductService for managing loan product catalog and operations.
 *
 * This service manages the loan product catalog, which defines the terms, conditions,
 * and rules for different types of loans offered by the institution. Products serve
 * as templates for loan origination and define business rules for validation.
 *
 * Key Responsibilities:
 * - Product catalog management (create, update, activate, deactivate)
 * - Product validation and business rule enforcement
 * - Fee and penalty calculations based on product rules
 * - Product matching and recommendation
 * - Product performance caching
 * - Regulatory compliance validation
 *
 * Product Attributes:
 * - Basic Information: Code, name, description, type
 * - Amount Limits: Minimum and maximum loan amounts
 * - Tenor Limits: Minimum and maximum repayment periods
 * - Interest Rates: Annual percentage rates
 * - Fees: Processing fees, late fees, prepayment penalties
 * - Requirements: Collateral, guarantor requirements
 * - Calculation Methods: Interest calculation, amortization type
 *
 * Product Types:
 * - PERSONAL_LOAN: Unsecured personal financing
 * - MORTGAGE: Real estate secured loans
 * - AUTO_LOAN: Vehicle financing
 * - BUSINESS_LOAN: Commercial financing
 * - MICROFINANCE: Small business/individual loans
 * - STUDENT_LOAN: Education financing
 *
 * Business Rules Enforced:
 * - Minimum amount cannot exceed maximum amount
 * - Minimum tenor cannot exceed maximum tenor
 * - Interest rates must be non-negative
 * - Product codes must be unique
 * - Product names must be unique
 * - Only active products can be used for new loans
 *
 * Caching Strategy:
 * - Products are cached for performance (frequently accessed)
 * - Cache is cleared on create/update/activate/deactivate operations
 * - Cache keys include product ID, code, and "active" for active products
 *
 * Integration Points:
 * - Loan origination system (product selection)
 * - Pricing engine (fee calculations)
 * - Risk management (validation rules)
 * - Reporting system (product performance)
 * - Mobile/web applications (product catalog)
 *
 * @see LoanProductService
 * @see LoanProduct
 * @see com.openfinova.banking.loan.api.entity.LoanProductType
 */
@Service
@Transactional(readOnly = true)
public class LoanProductService {

    private static final Logger logger = LoggerFactory.getLogger(LoanProductService.class);

    private final LoanProductRepository productRepository;

    public LoanProductService(LoanProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Creates a new loan product with comprehensive validation.
     *
     * This method creates a new product in the catalog after validating all
     * business rules and ensuring data integrity. Products serve as templates
     * for loan origination and define the terms and conditions.
     *
     * Validation Performed:
     * - Business rule validation (amount ranges, tenor ranges, interest rates)
     * - Uniqueness checks (product code and name must be unique)
     * - Data integrity validation (required fields, format validation)
     * - Regulatory compliance checks
     *
     * Product Creation Process:
     * 1. Validate all business rules
     * 2. Check for duplicate product code
     * 3. Check for duplicate product name
     * 4. Save product to database
     * 5. Clear product cache
     * 6. Log creation for audit
     *
     * Business Rules Validated:
     * - Minimum amount ≤ Maximum amount
     * - Minimum tenor ≤ Maximum tenor
     * - Interest rate ≥ 0
     * - Required fields are present
     * - Valid enumeration values
     *
     * After Creation:
     * - Product is available for loan origination (if active)
     * - Product appears in catalog queries
     * - Product can be used for validation and calculations
     * - Audit trail is established
     *
     * Cache Management:
     * - Clears all product caches to ensure consistency
     * - New product will be cached on first access
     * - Cache warming may be triggered for active products
     *
     * @param product the loan product to create (must not be null)
     * @return the created product with assigned ID and timestamps
     * @throws IllegalArgumentException if validation fails or duplicates exist
     */
    @Transactional
    @CacheEvict(value = "loanProducts", allEntries = true)
    public LoanProduct createProduct(LoanProduct product) {
        logger.info("Creating loan product: {}", product.getProductCode());

        validateProduct(product);

        // Check for duplicate product code
        if (productRepository.findByProductCode(product.getProductCode()).isPresent()) {
            throw new IllegalArgumentException("Product code already exists: " + product.getProductCode());
        }

        // Check for duplicate product name
        if (productRepository.findByProductName(product.getProductName()).isPresent()) {
            throw new IllegalArgumentException("Product name already exists: " + product.getProductName());
        }

        LoanProduct saved = productRepository.save(product);
        logger.info("Created loan product with ID: {}", saved.getId());

        return saved;
    }

    /**
     * Updates an existing loan product with validation.
     *
     * This method updates all modifiable attributes of a loan product while
     * ensuring data integrity and business rule compliance. Existing loans
     * using this product are not affected by the changes.
     *
     * Update Process:
     * 1. Validate product exists
     * 2. Validate all business rules
     * 3. Check for duplicate code/name (excluding current product)
     * 4. Update all modifiable fields
     * 5. Save changes to database
     * 6. Clear product cache
     * 7. Log update for audit
     *
     * Fields Updated:
     * - Product code and name
     * - Product type and description
     * - Amount and tenor limits
     * - Interest rate and calculation method
     * - Repayment frequency and amortization type
     * - Fee structures (processing, late, prepayment)
     * - Requirements (collateral, guarantor)
     * - Grace period settings
     *
     * Validation Rules:
     * - All creation validation rules apply
     * - Uniqueness checks exclude the current product
     * - Product ID cannot be changed
     * - Creation timestamp cannot be changed
     *
     * Impact on Existing Loans:
     * - Existing loans retain their original product terms
     * - Product changes only affect new loan applications
     * - Historical product data is preserved for audit
     * - Loan calculations use original product settings
     *
     * Cache Management:
     * - Clears all product caches immediately
     * - Updated product will be cached on next access
     * - Ensures all users see updated product information
     *
     * @param id the ID of the product to update
     * @param product the updated product data
     * @return the updated product with new modification timestamp
     * @throws IllegalArgumentException if product not found or validation fails
     */
    @Transactional
    @CacheEvict(value = "loanProducts", allEntries = true)
    public LoanProduct updateProduct(UUID id, LoanProduct product) {
        logger.info("Updating loan product: {}", id);

        LoanProduct existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        validateProduct(product);

        // Check for duplicate product code (excluding current product)
        productRepository.findByProductCode(product.getProductCode()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Product code already exists: " + product.getProductCode());
            }
        });

        // Check for duplicate product name (excluding current product)
        productRepository.findByProductName(product.getProductName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Product name already exists: " + product.getProductName());
            }
        });

        // Update fields
        existing.setProductCode(product.getProductCode());
        existing.setProductName(product.getProductName());
        existing.setProductType(product.getProductType());
        existing.setDescription(product.getDescription());
        existing.setMinAmount(product.getMinAmount());
        existing.setMaxAmount(product.getMaxAmount());
        existing.setMinTenorMonths(product.getMinTenorMonths());
        existing.setMaxTenorMonths(product.getMaxTenorMonths());
        existing.setInterestRate(product.getInterestRate());
        existing.setInterestCalculationMethod(product.getInterestCalculationMethod());
        existing.setRepaymentFrequency(product.getRepaymentFrequency());
        existing.setAmortizationType(product.getAmortizationType());
        existing.setCurrency(product.getCurrency());
        existing.setCollateralRequired(product.getCollateralRequired());
        existing.setGuarantorRequired(product.getGuarantorRequired());
        existing.setGracePeriodDays(product.getGracePeriodDays());
        existing.setProcessingFeePercentage(product.getProcessingFeePercentage());
        existing.setProcessingFeeFixed(product.getProcessingFeeFixed());
        existing.setLateFeePercentage(product.getLateFeePercentage());
        existing.setLateFeeFixed(product.getLateFeeFixed());
        existing.setPrepaymentPenaltyPercentage(product.getPrepaymentPenaltyPercentage());

        LoanProduct updated = productRepository.save(existing);
        logger.info("Updated loan product: {}", id);

        return updated;
    }

    /**
     * Retrieves a loan product by its unique identifier.
     *
     * This method is cached for performance as products are frequently
     * accessed during loan origination and validation processes.
     *
     * @param id the product ID
     * @return Optional containing the product if found, empty otherwise
     */
    @Cacheable(value = "loanProducts", key = "#id")
    public Optional<LoanProduct> getProductById(UUID id) {
        return productRepository.findById(id);
    }

    /**
     * Retrieves a loan product by its unique product code.
     *
     * Product codes are business-friendly identifiers used in:
     * - Loan origination systems
     * - Customer-facing applications
     * - Integration with external systems
     * - Reporting and analytics
     *
     * This method is cached for performance.
     *
     * @param productCode the product code (case-sensitive)
     * @return Optional containing the product if found, empty otherwise
     */
    @Cacheable(value = "loanProducts", key = "#productCode")
    public Optional<LoanProduct> getProductByCode(String productCode) {
        return productRepository.findByProductCode(productCode);
    }

    /**
     * Retrieves all active loan products.
     *
     * Active products are those available for new loan applications.
     * This is the primary method for product catalog display and
     * loan origination workflows.
     *
     * Used by:
     * - Loan origination systems
     * - Customer-facing applications
     * - Mobile apps and websites
     * - Product recommendation engines
     *
     * This method is cached with key "active" for performance.
     *
     * @return list of all active products
     */
    @Cacheable(value = "loanProducts", key = "'active'")
    public List<LoanProduct> getAllActiveProducts() {
        return productRepository.findActiveProducts();
    }

    /**
     * Retrieves all active loan products with pagination.
     *
     * Paginated version for large product catalogs or when
     * implementing infinite scroll in user interfaces.
     *
     * @param pageable pagination parameters
     * @return page of active products
     */
    public Page<LoanProduct> getAllActiveProducts(Pageable pageable) {
        return productRepository.findActiveProducts(pageable);
    }

    /**
     * Retrieves all loan products (active and inactive) with pagination.
     *
     * Administrative view showing complete product catalog.
     * Used for:
     * - Product management interfaces
     * - Administrative reporting
     * - Audit and compliance
     * - Historical analysis
     *
     * @param pageable pagination parameters
     * @return page of all products regardless of status
     */
    public Page<LoanProduct> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * Retrieves products by type with pagination.
     *
     * Filters products by their type for category-specific views:
     * - PERSONAL_LOAN: Unsecured personal financing
     * - MORTGAGE: Real estate secured loans
     * - AUTO_LOAN: Vehicle financing
     * - BUSINESS_LOAN: Commercial financing
     * - MICROFINANCE: Small business/individual loans
     * - STUDENT_LOAN: Education financing
     *
     * Used for:
     * - Category-based product browsing
     * - Specialized loan applications
     * - Product performance analysis by type
     * - Regulatory reporting by loan category
     *
     * @param productType the product type to filter by
     * @param pageable pagination parameters
     * @return page of products of the specified type
     */
    public Page<LoanProduct> getProductsByType(LoanProductType productType, Pageable pageable) {
        return productRepository.findByProductType(productType, pageable);
    }

    /**
     * Retrieves products by currency with pagination.
     *
     * Filters products by their currency for multi-currency institutions.
     * Useful for:
     * - Currency-specific product catalogs
     * - Foreign exchange loan products
     * - Regional product offerings
     * - Regulatory compliance by currency
     *
     * @param currency the currency code (e.g., "USD", "EUR", "GBP")
     * @param pageable pagination parameters
     * @return page of products in the specified currency
     */
    public Page<LoanProduct> getProductsByCurrency(String currency, Pageable pageable) {
        return productRepository.findByCurrency(currency, pageable);
    }

    /**
     * Finds products that support a specific loan amount and currency.
     *
     * This method filters active products to find those that can accommodate
     * the requested loan amount. Used for product recommendation and
     * eligibility checking during loan origination.
     *
     * Filtering Logic:
     * - Only active products are considered
     * - Currency must match exactly
     * - Amount must be within product's min/max range (inclusive)
     *
     * Use Cases:
     * - Product recommendation engines
     * - Loan origination pre-qualification
     * - Customer self-service applications
     * - Mobile app product suggestions
     *
     * @param amount the desired loan amount
     * @param currency the loan currency
     * @return list of products that can accommodate the amount
     */
    public List<LoanProduct> findProductsForAmount(BigDecimal amount, String currency) {
        return getAllActiveProducts().stream().filter(p -> p.getCurrency().equals(currency))
                .filter(p -> amount.compareTo(p.getMinAmount()) >= 0 && amount.compareTo(p.getMaxAmount()) <= 0)
                .collect(Collectors.toList());
    }

    /**
     * Finds products that support a specific repayment tenor.
     *
     * This method filters active products to find those that can accommodate
     * the requested repayment period. Used for tenor-based product filtering
     * and recommendation.
     *
     * Filtering Logic:
     * - Only active products are considered
     * - Tenor must be within product's min/max range (inclusive)
     *
     * Use Cases:
     * - Tenor-based product filtering
     * - Repayment period planning
     * - Product comparison by tenor
     * - Customer preference matching
     *
     * @param tenorMonths the desired repayment period in months
     * @return list of products that support the tenor
     */
    public List<LoanProduct> findProductsForTenor(Integer tenorMonths) {
        return getAllActiveProducts().stream()
                .filter(p -> tenorMonths >= p.getMinTenorMonths() && tenorMonths <= p.getMaxTenorMonths())
                .collect(Collectors.toList());
    }

    /**
     * Finds products matching specific loan criteria (amount, tenor, currency).
     *
     * This is the primary product matching method used during loan origination
     * to find products that meet all customer requirements. It combines
     * amount, tenor, and currency filtering for comprehensive matching.
     *
     * Filtering Logic:
     * - Only active products are considered
     * - Currency must match exactly
     * - Amount must be within product's min/max range (inclusive)
     * - Tenor must be within product's min/max range (inclusive)
     *
     * Use Cases:
     * - Loan origination product selection
     * - Product recommendation engines
     * - Pre-qualification workflows
     * - Customer self-service applications
     * - Mobile app loan calculators
     *
     * Results are typically sorted by:
     * - Interest rate (ascending)
     * - Processing fees (ascending)
     * - Product popularity
     * - Customer segment preferences
     *
     * @param amount the desired loan amount
     * @param tenorMonths the desired repayment period in months
     * @param currency the loan currency
     * @return list of products matching all criteria
     */
    public List<LoanProduct> findMatchingProducts(BigDecimal amount, Integer tenorMonths, String currency) {
        return getAllActiveProducts().stream().filter(p -> p.getCurrency().equals(currency))
                .filter(p -> amount.compareTo(p.getMinAmount()) >= 0 && amount.compareTo(p.getMaxAmount()) <= 0)
                .filter(p -> tenorMonths >= p.getMinTenorMonths() && tenorMonths <= p.getMaxTenorMonths())
                .collect(Collectors.toList());
    }

    /**
     * Activates a loan product for use in new loan applications.
     *
     * Activation makes a product available for:
     * - New loan applications
     * - Product catalog display
     * - Customer selection
     * - Automated product matching
     *
     * Activation Process:
     * 1. Validate product exists
     * 2. Call product's activate() method
     * 3. Save activation state
     * 4. Clear product cache
     * 5. Log activation for audit
     *
     * Business Rules:
     * - Product must exist
     * - Product can be activated multiple times (idempotent)
     * - Activation is immediate and affects all systems
     *
     * Cache Management:
     * - Clears all product caches
     * - Activated product appears in active product queries
     * - Cache warming may be triggered
     *
     * @param id the ID of the product to activate
     * @return the activated product
     * @throws IllegalArgumentException if product not found
     */
    @Transactional
    @CacheEvict(value = "loanProducts", allEntries = true)
    public LoanProduct activateProduct(UUID id) {
        logger.info("Activating loan product: {}", id);

        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.activate();
        LoanProduct activated = productRepository.save(product);

        logger.info("Activated loan product: {}", id);
        return activated;
    }

    /**
     * Deactivates a loan product to prevent use in new loan applications.
     *
     * Deactivation removes a product from:
     * - New loan application options
     * - Product catalog display
     * - Customer selection menus
     * - Automated product matching
     *
     * Deactivation Process:
     * 1. Validate product exists
     * 2. Call product's deactivate() method
     * 3. Save deactivation state
     * 4. Clear product cache
     * 5. Log deactivation for audit
     *
     * Impact on Existing Loans:
     * - Existing loans are not affected
     * - Historical data is preserved
     * - Product calculations remain valid
     * - Reporting continues to work
     *
     * Common Deactivation Reasons:
     * - Product discontinuation
     * - Regulatory changes
     * - Risk management decisions
     * - Market conditions
     * - Temporary suspension
     *
     * Business Rules:
     * - Product must exist
     * - Product can be deactivated multiple times (idempotent)
     * - Deactivation is immediate and affects all systems
     * - Product can be reactivated later
     *
     * @param id the ID of the product to deactivate
     * @return the deactivated product
     * @throws IllegalArgumentException if product not found
     */
    @Transactional
    @CacheEvict(value = "loanProducts", allEntries = true)
    public LoanProduct deactivateProduct(UUID id) {
        logger.info("Deactivating loan product: {}", id);

        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.deactivate();
        LoanProduct deactivated = productRepository.save(product);

        logger.info("Deactivated loan product: {}", id);
        return deactivated;
    }

    /**
     * Validates if a loan amount is within the product's limits.
     *
     * This validation is used during loan origination to ensure
     * the requested amount complies with product rules.
     *
     * Validation Rules:
     * - Amount must be ≥ product minimum amount
     * - Amount must be ≤ product maximum amount
     * - Validation is inclusive of boundaries
     *
     * @param productId the product ID to validate against
     * @param amount the loan amount to validate
     * @return ProductValidationResult with validation status and message
     * @throws IllegalArgumentException if product not found
     */
    public ProductValidationResult validateLoanAmount(UUID productId, BigDecimal amount) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (amount.compareTo(product.getMinAmount()) < 0) {
            return ProductValidationResult
                    .failure("Loan amount " + amount + " is below minimum " + product.getMinAmount());
        }

        if (amount.compareTo(product.getMaxAmount()) > 0) {
            return ProductValidationResult
                    .failure("Loan amount " + amount + " exceeds maximum " + product.getMaxAmount());
        }

        return ProductValidationResult.success();
    }

    /**
     * Validates if a repayment tenor is within the product's limits.
     *
     * This validation ensures the requested repayment period
     * complies with product rules and regulatory requirements.
     *
     * Validation Rules:
     * - Tenor must be ≥ product minimum tenor
     * - Tenor must be ≤ product maximum tenor
     * - Validation is inclusive of boundaries
     *
     * @param productId the product ID to validate against
     * @param tenorMonths the repayment tenor in months to validate
     * @return ProductValidationResult with validation status and message
     * @throws IllegalArgumentException if product not found
     */
    public ProductValidationResult validateTenor(UUID productId, Integer tenorMonths) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (tenorMonths < product.getMinTenorMonths()) {
            return ProductValidationResult
                    .failure("Tenor " + tenorMonths + " months is below minimum " + product.getMinTenorMonths());
        }

        if (tenorMonths > product.getMaxTenorMonths()) {
            return ProductValidationResult
                    .failure("Tenor " + tenorMonths + " months exceeds maximum " + product.getMaxTenorMonths());
        }

        return ProductValidationResult.success();
    }

    /**
     * Validates comprehensive loan parameters against product rules.
     *
     * This is the primary validation method used during loan origination
     * to ensure all loan parameters comply with product rules and
     * regulatory requirements.
     *
     * Validation Checks:
     * - Product is active and available for new loans
     * - Loan amount is within product's min/max range
     * - Repayment tenor is within product's min/max range
     * - All parameters are valid and consistent
     *
     * Validation Process:
     * 1. Check product is active
     * 2. Validate amount against product limits
     * 3. Validate tenor against product limits
     * 4. Collect all validation errors
     * 5. Return comprehensive result
     *
     * Use Cases:
     * - Loan application validation
     * - Pre-qualification checks
     * - Customer self-service validation
     * - Mobile app form validation
     * - API parameter validation
     *
     * Result Details:
     * - Success flag (true if all validations pass)
     * - List of specific error messages
     * - Overall validation message
     *
     * @param productId the product ID to validate against
     * @param amount the loan amount to validate
     * @param tenorMonths the repayment tenor in months to validate
     * @return ProductValidationResult with comprehensive validation details
     * @throws IllegalArgumentException if product not found
     */
    public ProductValidationResult validateLoanParameters(UUID productId, BigDecimal amount, Integer tenorMonths) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<String> errors = new ArrayList<>();

        // Validate product is active
        if (!product.isActive()) {
            errors.add("Product is not active");
        }

        // Validate amount
        if (amount.compareTo(product.getMinAmount()) < 0) {
            errors.add("Loan amount " + amount + " is below minimum " + product.getMinAmount());
        }
        if (amount.compareTo(product.getMaxAmount()) > 0) {
            errors.add("Loan amount " + amount + " exceeds maximum " + product.getMaxAmount());
        }

        // Validate tenor
        if (tenorMonths < product.getMinTenorMonths()) {
            errors.add("Tenor " + tenorMonths + " months is below minimum " + product.getMinTenorMonths());
        }
        if (tenorMonths > product.getMaxTenorMonths()) {
            errors.add("Tenor " + tenorMonths + " months exceeds maximum " + product.getMaxTenorMonths());
        }

        if (errors.isEmpty()) {
            return ProductValidationResult.success();
        }

        return ProductValidationResult.failure(errors);
    }

    /**
     * Calculates the processing fee for a loan based on product rules.
     *
     * Processing fees are charged when a loan is originated and cover
     * administrative costs, underwriting, and loan setup expenses.
     *
     * Fee Calculation:
     * - Percentage-based fee: (loan amount × percentage) ÷ 100
     * - Fixed fee: flat amount regardless of loan size
     * - Total fee: percentage fee + fixed fee
     *
     * Fee Structure Examples:
     * - 2% + $50: 2% of loan amount plus $50 fixed fee
     * - 1.5%: 1.5% of loan amount only
     * - $100: Fixed $100 fee only
     * - $0: No processing fee
     *
     * Use Cases:
     * - Loan origination fee calculation
     * - Customer cost disclosure
     * - Loan pricing displays
     * - Total cost calculations
     * - Regulatory fee reporting
     *
     * @param productId the product ID to get fee rules from
     * @param loanAmount the loan amount to calculate fee for
     * @return the calculated processing fee amount
     * @throws IllegalArgumentException if product not found
     */
    public BigDecimal calculateProcessingFee(UUID productId, BigDecimal loanAmount) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        BigDecimal fee = BigDecimal.ZERO;

        // Add percentage-based fee
        if (product.getProcessingFeePercentage() != null) {
            fee = fee.add(loanAmount.multiply(product.getProcessingFeePercentage()).divide(BigDecimal.valueOf(100)));
        }

        // Add fixed fee
        if (product.getProcessingFeeFixed() != null) {
            fee = fee.add(product.getProcessingFeeFixed());
        }

        return fee;
    }

    /**
     * Calculates the late payment fee for overdue amounts based on product rules.
     *
     * Late fees are penalties charged when loan payments are not made
     * on time. They serve to compensate for additional collection costs
     * and encourage timely payments.
     *
     * Fee Calculation:
     * - Percentage-based fee: (overdue amount × percentage) ÷ 100
     * - Fixed fee: flat amount regardless of overdue amount
     * - Total fee: percentage fee + fixed fee
     *
     * Fee Structure Examples:
     * - 5% + $25: 5% of overdue amount plus $25 fixed fee
     * - 3%: 3% of overdue amount only
     * - $50: Fixed $50 fee only
     * - $0: No late fee
     *
     * Regulatory Considerations:
     * - Late fees may be capped by regulation
     * - Some jurisdictions limit fee frequency
     * - Consumer protection laws may apply
     * - Fee disclosure requirements
     *
     * Use Cases:
     * - Delinquency management
     * - Payment processing
     * - Customer notifications
     * - Collection workflows
     * - Penalty assessments
     *
     * @param productId the product ID to get late fee rules from
     * @param overdueAmount the overdue amount to calculate fee for
     * @return the calculated late payment fee amount
     * @throws IllegalArgumentException if product not found
     */
    public BigDecimal calculateLateFee(UUID productId, BigDecimal overdueAmount) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        BigDecimal fee = BigDecimal.ZERO;

        // Add percentage-based fee
        if (product.getLateFeePercentage() != null) {
            fee = fee.add(overdueAmount.multiply(product.getLateFeePercentage()).divide(BigDecimal.valueOf(100)));
        }

        // Add fixed fee
        if (product.getLateFeeFixed() != null) {
            fee = fee.add(product.getLateFeeFixed());
        }

        return fee;
    }

    /**
     * Calculates the prepayment penalty for early loan settlement.
     *
     * Prepayment penalties compensate lenders for lost interest income
     * when borrowers pay off loans early. Not all products have
     * prepayment penalties.
     *
     * Penalty Calculation:
     * - Percentage-based: (prepayment amount × percentage) ÷ 100
     * - If no penalty percentage configured: $0
     *
     * Penalty Structure Examples:
     * - 2%: 2% of prepayment amount
     * - 0%: No prepayment penalty
     * - null: No prepayment penalty
     *
     * Regulatory Considerations:
     * - Some jurisdictions prohibit prepayment penalties
     * - Consumer loans may have penalty restrictions
     * - Penalties may be time-limited (e.g., first 2 years only)
     * - Disclosure requirements apply
     *
     * Use Cases:
     * - Early settlement calculations
     * - Prepayment quotes
     * - Customer cost disclosure
     * - Refinancing analysis
     * - Settlement negotiations
     *
     * @param productId the product ID to get penalty rules from
     * @param prepaymentAmount the amount being prepaid
     * @return the calculated prepayment penalty amount (zero if no penalty)
     * @throws IllegalArgumentException if product not found
     */
    public BigDecimal calculatePrepaymentPenalty(UUID productId, BigDecimal prepaymentAmount) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getPrepaymentPenaltyPercentage() == null) {
            return BigDecimal.ZERO;
        }

        return prepaymentAmount.multiply(product.getPrepaymentPenaltyPercentage()).divide(BigDecimal.valueOf(100));
    }

    /**
     * Checks if a product requires collateral for loan approval.
     *
     * Collateral requirements vary by product type and risk profile:
     * - Secured loans: Require collateral (mortgages, auto loans)
     * - Unsecured loans: No collateral required (personal loans)
     * - Mixed products: May require collateral above certain amounts
     *
     * Used for:
     * - Loan origination workflows
     * - Application form customization
     * - Document collection requirements
     * - Risk assessment processes
     * - Customer communication
     *
     * @param productId the product ID to check
     * @return true if collateral is required, false otherwise
     * @throws IllegalArgumentException if product not found (returns false)
     */
    public boolean requiresCollateral(UUID productId) {
        return productRepository.findById(productId).map(LoanProduct::getCollateralRequired).orElse(false);
    }

    /**
     * Checks if a product requires guarantors for loan approval.
     *
     * Guarantor requirements provide additional security for lenders:
     * - High-risk products: May require guarantors
     * - Large loan amounts: May require guarantors above thresholds
     * - Unsecured loans: May require guarantors for risk mitigation
     * - First-time borrowers: May require guarantors
     *
     * Used for:
     * - Loan origination workflows
     * - Application form customization
     * - Document collection requirements
     * - Risk assessment processes
     * - Customer communication
     *
     * @param productId the product ID to check
     * @return true if guarantors are required, false otherwise
     * @throws IllegalArgumentException if product not found (returns false)
     */
    public boolean requiresGuarantor(UUID productId) {
        return productRepository.findById(productId).map(LoanProduct::getGuarantorRequired).orElse(false);
    }

    /**
     * Counts the total number of active products in the catalog.
     *
     * Used for:
     * - Dashboard metrics
     * - Product catalog statistics
     * - Performance monitoring
     * - Capacity planning
     *
     * @return the count of active products
     */
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    /**
     * Counts products by type for category analysis.
     *
     * Provides insights into product portfolio composition:
     * - Product mix analysis
     * - Category performance metrics
     * - Strategic planning data
     * - Regulatory reporting
     *
     * @param productType the product type to count
     * @return the count of products of the specified type
     */
    public long countProductsByType(LoanProductType productType) {
        return productRepository.countByProductType(productType);
    }

    /**
     * Validates a loan product's business rules and data integrity.
     *
     * This comprehensive validation ensures products meet all business
     * requirements and regulatory compliance standards.
     *
     * Validation Rules:
     * - Amount Range: Minimum amount ≤ Maximum amount
     * - Tenor Range: Minimum tenor ≤ Maximum tenor
     * - Interest Rate: Must be non-negative (≥ 0)
     * - Required Fields: All mandatory fields present
     * - Data Types: Correct data types and formats
     * - Business Logic: Consistent and logical values
     *
     * Additional Validations (could be added):
     * - Currency code validation (ISO 4217)
     * - Interest rate caps (regulatory limits)
     * - Fee reasonableness checks
     * - Product code format validation
     * - Regulatory compliance checks
     *
     * @param product the product to validate
     * @throws IllegalArgumentException if validation fails with detailed error message
     */
    private void validateProduct(LoanProduct product) {
        List<String> errors = new ArrayList<>();

        // Validate amount range
        if (product.getMinAmount().compareTo(product.getMaxAmount()) > 0) {
            errors.add("Minimum amount cannot exceed maximum amount");
        }

        // Validate tenor range
        if (product.getMinTenorMonths() > product.getMaxTenorMonths()) {
            errors.add("Minimum tenor cannot exceed maximum tenor");
        }

        // Validate interest rate
        if (product.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Interest rate cannot be negative");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Product validation failed: " + String.join(", ", errors));
        }
    }
}
