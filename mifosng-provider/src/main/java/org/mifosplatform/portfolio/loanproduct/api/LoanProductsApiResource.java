package org.mifosplatform.portfolio.loanproduct.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.EntityIdentifier;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.monetary.data.CurrencyData;
import org.mifosplatform.organisation.monetary.service.CurrencyReadPlatformService;
import org.mifosplatform.portfolio.charge.data.ChargeData;
import org.mifosplatform.portfolio.charge.service.ChargeReadPlatformService;
import org.mifosplatform.portfolio.fund.data.FundData;
import org.mifosplatform.portfolio.fund.service.FundReadPlatformService;
import org.mifosplatform.portfolio.loanproduct.data.LoanProductData;
import org.mifosplatform.portfolio.loanproduct.data.TransactionProcessingStrategyData;
import org.mifosplatform.portfolio.loanproduct.service.LoanDropdownReadPlatformService;
import org.mifosplatform.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/loanproducts")
@Component
@Scope("singleton")
public class LoanProductsApiResource {

    private final Set<String> LOAN_PRODUCT_DATA_PARAMETERS = new HashSet<String>(Arrays.asList("id", "name", "description", "fundId",
            "fundName", "transactionProcessingStrategyId", "transactionProcessingStrategyName", "principal", "inArrearsTolerance",
            "numberOfRepayments", "repaymentEvery", "interestRatePerPeriod", "annualInterestRate", "repaymentFrequencyType",
            "interestRateFrequencyType", "amortizationType", "interestType", "interestCalculationPeriodType", "charges", "createdOn",
            "lastModifedOn", "currencyOptions", "amortizationTypeOptions", "interestTypeOptions", "interestCalculationPeriodTypeOptions",
            "repaymentFrequencyTypeOptions", "interestRateFrequencyTypeOptions", "fundOptions", "transactionProcessingStrategyOptions",
            "chargeOptions"));

    private final String resourceNameForPermissions = "LOANPRODUCT";

    private final PlatformSecurityContext context;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final FundReadPlatformService fundReadPlatformService;
    private final DefaultToApiJsonSerializer<LoanProductData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final LoanDropdownReadPlatformService dropdownReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public LoanProductsApiResource(final PlatformSecurityContext context, final LoanProductReadPlatformService readPlatformService,
            final ChargeReadPlatformService chargeReadPlatformService, final CurrencyReadPlatformService currencyReadPlatformService,
            final FundReadPlatformService fundReadPlatformService, final LoanDropdownReadPlatformService dropdownReadPlatformService,
            final DefaultToApiJsonSerializer<LoanProductData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.loanProductReadPlatformService = readPlatformService;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.fundReadPlatformService = fundReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createLoanProduct(final String apiRequestBodyAsJson) {

        final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("CREATE", "LOANPRODUCT", "CREATE",
                "loanproducts", null, apiRequestBodyAsJson);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllLoanProducts(@Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final Collection<LoanProductData> products = this.loanProductReadPlatformService.retrieveAllLoanProducts();

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, products, LOAN_PRODUCT_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveNewLoanProductDetails(@Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        LoanProductData loanProduct = this.loanProductReadPlatformService.retrieveNewLoanProductDetails();
        loanProduct = handleTemplate(loanProduct);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanProduct, LOAN_PRODUCT_DATA_PARAMETERS);
    }

    @GET
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanProductDetails(@PathParam("productId") final Long productId, @Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        LoanProductData loanProduct = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
        if (settings.isTemplate()) {
            loanProduct = handleTemplate(loanProduct);
        }

        return this.toApiJsonSerializer.serialize(settings, loanProduct, LOAN_PRODUCT_DATA_PARAMETERS);
    }

    @PUT
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateLoanProduct(@PathParam("productId") final Long productId, final String apiRequestBodyAsJson) {

        final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("UPDATE", "LOANPRODUCT", "UPDATE",
                "loanproducts", productId, apiRequestBodyAsJson);

        return this.toApiJsonSerializer.serialize(result);
    }

    private LoanProductData handleTemplate(final LoanProductData productData) {

        final boolean feeChargesOnly = true;
        Collection<ChargeData> chargeOptions = this.chargeReadPlatformService.retrieveLoanApplicableCharges(feeChargesOnly);
        chargeOptions.removeAll(productData.charges());

        final List<CurrencyData> currencyOptions = currencyReadPlatformService.retrieveAllowedCurrencies();
        final List<EnumOptionData> amortizationTypeOptions = dropdownReadPlatformService.retrieveLoanAmortizationTypeOptions();
        final List<EnumOptionData> interestTypeOptions = dropdownReadPlatformService.retrieveLoanInterestTypeOptions();
        final List<EnumOptionData> interestCalculationPeriodTypeOptions = dropdownReadPlatformService
                .retrieveLoanInterestRateCalculatedInPeriodOptions();
        final List<EnumOptionData> loanTermFrequencyTypeOptions = dropdownReadPlatformService.retrieveLoanTermFrequencyTypeOptions();
        final List<EnumOptionData> repaymentFrequencyTypeOptions = dropdownReadPlatformService.retrieveRepaymentFrequencyTypeOptions();
        final List<EnumOptionData> interestRateFrequencyTypeOptions = dropdownReadPlatformService
                .retrieveInterestRateFrequencyTypeOptions();

        final Collection<FundData> fundOptions = this.fundReadPlatformService.retrieveAllFunds();
        final Collection<TransactionProcessingStrategyData> transactionProcessingStrategyOptions = this.dropdownReadPlatformService
                .retreiveTransactionProcessingStrategies();

        return new LoanProductData(productData, chargeOptions, currencyOptions, amortizationTypeOptions, interestTypeOptions,
                interestCalculationPeriodTypeOptions, loanTermFrequencyTypeOptions, repaymentFrequencyTypeOptions,
                interestRateFrequencyTypeOptions, fundOptions, transactionProcessingStrategyOptions);
    }
}