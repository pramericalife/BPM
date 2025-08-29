package script;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.example.ProposalInfo;
import com.example.SupplierInfo;

import org.kie.api.runtime.process.ProcessContext;

/**
 * OrderManagementScript
 */
@SuppressWarnings("unchecked")
public class OrderManagementScript {

    public static void requestOfferExit(ProcessContext kcontext) {
        ProposalInfo orderInfo = (ProposalInfo) kcontext.getVariable("orderInfo");
        kcontext.setVariable("suppliersList", orderInfo.getSuppliersList());
        kcontext.setVariable("suppliersInfoList", new ArrayList<SupplierInfo>());
    }

    public static void prepareOfferExit(ProcessContext kcontext) {
        SupplierInfo supplierInfoOut = (SupplierInfo) kcontext.getVariable("supplierInfoOut");
        supplierInfoOut.setUser((String) kcontext.getVariable("supplier"));
        kcontext.setVariable("supplierInfoOut", supplierInfoOut);
        System.out.println("prepareOfferExit: " + supplierInfoOut);
    }

    // Auto Approval Rules for initiation
    public static void autoApprovalRulesEntry(ProcessContext kcontext) {
        // select the lowest offer
        List<SupplierInfo> suppliersInfoList = (List<SupplierInfo>) kcontext.getVariable("suppliersInfoList");
        Optional<SupplierInfo> supplierInfoOpt = suppliersInfoList.stream()
                .min(Comparator.comparing(SupplierInfo::getOffer));

        if (supplierInfoOpt.isPresent()) {
            kcontext.setVariable("supplierInfo", supplierInfoOpt.get());

            ProposalInfo orderInfo = (ProposalInfo) kcontext.getVariable("orderInfo");

            orderInfo.setPrice(supplierInfoOpt.get().getOffer());
            kcontext.setVariable("orderInfo", orderInfo);
        }
    }
}