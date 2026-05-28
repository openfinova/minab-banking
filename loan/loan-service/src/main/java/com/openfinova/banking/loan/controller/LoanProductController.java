package com.openfinova.banking.loan.controller;

import com.openfinova.banking.loan.api.dto.LoanProductRequest;
import com.openfinova.banking.loan.api.dto.LoanProductResponse;
import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.dto.ProductValidationResult;
import com.openfinova.banking.loan.entity.LoanProduct;
import com.openfinova.banking.loan.mapper.LoanProductMapper;
import com.openfinova.banking.loan.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing loan products.
 */
@RestController
@RequestMapping("/api/v1/loan-products")
@Tag(name = "Loan Products", description = "Loan product management APIs")
public class LoanProductController {

    private final LoanProductService productService;

    public LoanProductController(LoanProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Create a new loan product")
    public ResponseEntity<LoanProductResponse> createProduct(@Valid @RequestBody LoanProductRequest request) {

        LoanProduct product = LoanProductMapper.toEntity(request);
        LoanProduct created = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanProductMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Update an existing loan product")
    public ResponseEntity<LoanProductResponse> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody LoanProductRequest request) {

        LoanProduct product = LoanProductMapper.toEntity(request);
        LoanProduct updated = productService.updateProduct(id, product);

        return ResponseEntity.ok(LoanProductMapper.toResponse(updated));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get loan product by ID")
    public ResponseEntity<LoanProductResponse> getProductById(@PathVariable UUID id) {
        return productService.getProductById(id).map(LoanProductMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{productCode}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get loan product by product code")
    public ResponseEntity<LoanProductResponse> getProductByCode(@PathVariable String productCode) {

        return productService.getProductByCode(productCode).map(LoanProductMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all loan products with pagination")
    public ResponseEntity<Page<LoanProductResponse>> getAllProducts(
            @Parameter(description = "Include inactive products") @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {

        Page<LoanProduct> products = includeInactive ? productService.getAllProducts(pageable)
                : productService.getAllActiveProducts(pageable);

        return ResponseEntity.ok(products.map(LoanProductMapper::toResponse));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all active loan products")
    public ResponseEntity<List<LoanProductResponse>> getAllActiveProducts() {
        List<LoanProduct> products = productService.getAllActiveProducts();
        List<LoanProductResponse> responses = products.stream().map(LoanProductMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/type/{productType}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get loan products by type")
    public ResponseEntity<Page<LoanProductResponse>> getProductsByType(@PathVariable LoanProductType productType,
            Pageable pageable) {

        Page<LoanProduct> products = productService.getProductsByType(productType, pageable);
        return ResponseEntity.ok(products.map(LoanProductMapper::toResponse));
    }

    @GetMapping("/currency/{currency}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get loan products by currency")
    public ResponseEntity<Page<LoanProductResponse>> getProductsByCurrency(@PathVariable String currency,
            Pageable pageable) {

        Page<LoanProduct> products = productService.getProductsByCurrency(currency, pageable);
        return ResponseEntity.ok(products.map(LoanProductMapper::toResponse));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Find loan products matching criteria")
    public ResponseEntity<List<LoanProductResponse>> findMatchingProducts(@RequestParam BigDecimal amount,
            @RequestParam Integer tenorMonths, @RequestParam String currency) {

        List<LoanProduct> products = productService.findMatchingProducts(amount, tenorMonths, currency);
        List<LoanProductResponse> responses = products.stream().map(LoanProductMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Activate a loan product")
    public ResponseEntity<LoanProductResponse> activateProduct(@PathVariable UUID id) {
        LoanProduct activated = productService.activateProduct(id);
        return ResponseEntity.ok(LoanProductMapper.toResponse(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Deactivate a loan product")
    public ResponseEntity<LoanProductResponse> deactivateProduct(@PathVariable UUID id) {
        LoanProduct deactivated = productService.deactivateProduct(id);
        return ResponseEntity.ok(LoanProductMapper.toResponse(deactivated));
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Validate loan parameters against product")
    public ResponseEntity<ProductValidationResult> validateLoanParameters(@PathVariable UUID id,
            @RequestParam BigDecimal amount, @RequestParam Integer tenorMonths) {

        ProductValidationResult result = productService.validateLoanParameters(id, amount, tenorMonths);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/fees/processing")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate processing fee")
    public ResponseEntity<BigDecimal> calculateProcessingFee(@PathVariable UUID id,
            @RequestParam BigDecimal loanAmount) {

        BigDecimal fee = productService.calculateProcessingFee(id, loanAmount);
        return ResponseEntity.ok(fee);
    }

    @GetMapping("/{id}/fees/late")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate late payment fee")
    public ResponseEntity<BigDecimal> calculateLateFee(@PathVariable UUID id, @RequestParam BigDecimal overdueAmount) {

        BigDecimal fee = productService.calculateLateFee(id, overdueAmount);
        return ResponseEntity.ok(fee);
    }

    @GetMapping("/{id}/fees/prepayment")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate prepayment penalty")
    public ResponseEntity<BigDecimal> calculatePrepaymentPenalty(@PathVariable UUID id,
            @RequestParam BigDecimal prepaymentAmount) {

        BigDecimal penalty = productService.calculatePrepaymentPenalty(id, prepaymentAmount);
        return ResponseEntity.ok(penalty);
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get count of active products")
    public ResponseEntity<Long> countActiveProducts() {
        return ResponseEntity.ok(productService.countActiveProducts());
    }

    @GetMapping("/stats/count/{productType}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get count of products by type")
    public ResponseEntity<Long> countProductsByType(@PathVariable LoanProductType productType) {
        return ResponseEntity.ok(productService.countProductsByType(productType));
    }
}
