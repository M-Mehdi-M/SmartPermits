package project.smartpermits;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class PermitTypeHelper {

    private static final Map<String, Integer> PERMIT_TYPE_MAP = new LinkedHashMap<>();
    static {
        PERMIT_TYPE_MAP.put("Construction Permit", R.string.permit_construction);
        PERMIT_TYPE_MAP.put("Renovation Permit", R.string.permit_renovation);
        PERMIT_TYPE_MAP.put("Business License", R.string.permit_business);
        PERMIT_TYPE_MAP.put("Food Service Permit", R.string.permit_food);
        PERMIT_TYPE_MAP.put("Event Permit", R.string.permit_event);
        PERMIT_TYPE_MAP.put("Signage Permit", R.string.permit_signage);
        PERMIT_TYPE_MAP.put("Demolition Permit", R.string.permit_demolition);
        PERMIT_TYPE_MAP.put("Occupancy Certificate", R.string.permit_occupancy);
    }

    private static final Map<String, Integer> DOC_NAME_MAP = new LinkedHashMap<>();
    static {
        DOC_NAME_MAP.put("Urban Planning Certificate", R.string.doc_urban_planning);
        DOC_NAME_MAP.put("Land Registry Extract", R.string.doc_land_registry);
        DOC_NAME_MAP.put("Topographic Survey Plan", R.string.doc_topo_survey);
        DOC_NAME_MAP.put("Authorized Technical Project", R.string.doc_tech_project);
        DOC_NAME_MAP.put("Utility Approvals (Water, Gas, Electricity)", R.string.doc_utility_approvals);
        DOC_NAME_MAP.put("Geotechnical Study", R.string.doc_geotech_study);
        DOC_NAME_MAP.put("Fee Payment Proof", R.string.doc_fee_proof);
        DOC_NAME_MAP.put("Existing Condition Survey", R.string.doc_existing_survey);
        DOC_NAME_MAP.put("Renovation Technical Project", R.string.doc_renovation_project);
        DOC_NAME_MAP.put("Homeowners Association Approval (if applicable)", R.string.doc_hoa_approval);
        DOC_NAME_MAP.put("Affected Utility Approvals", R.string.doc_affected_utility);
        DOC_NAME_MAP.put("Business Registration Certificate", R.string.doc_biz_registration);
        DOC_NAME_MAP.put("Articles of Incorporation", R.string.doc_articles_inc);
        DOC_NAME_MAP.put("Office Space Lease Agreement", R.string.doc_lease_agreement);
        DOC_NAME_MAP.put("Fire Safety Approval", R.string.doc_fire_safety);
        DOC_NAME_MAP.put("Tax Clearance Certificate", R.string.doc_tax_clearance);
        DOC_NAME_MAP.put("Business Registry Certificate", R.string.doc_biz_registry);
        DOC_NAME_MAP.put("Veterinary Sanitary Authorization", R.string.doc_vet_sanitary);
        DOC_NAME_MAP.put("HACCP Plan", R.string.doc_haccp);
        DOC_NAME_MAP.put("Pest Control Service Contract", R.string.doc_pest_control);
        DOC_NAME_MAP.put("Environmental Approval", R.string.doc_env_approval);
        DOC_NAME_MAP.put("Water Quality Analysis Report", R.string.doc_water_quality);
        DOC_NAME_MAP.put("Event Organization Request", R.string.doc_event_request);
        DOC_NAME_MAP.put("Security Plan", R.string.doc_security_plan);
        DOC_NAME_MAP.put("Police Approval", R.string.doc_police_approval);
        DOC_NAME_MAP.put("Fire Department Approval", R.string.doc_fire_dept);
        DOC_NAME_MAP.put("Sanitation Service Contract", R.string.doc_sanitation);
        DOC_NAME_MAP.put("Liability Insurance Policy", R.string.doc_liability_insurance);
        DOC_NAME_MAP.put("Signage Placement Request", R.string.doc_signage_request);
        DOC_NAME_MAP.put("Site Sketch", R.string.doc_site_sketch);
        DOC_NAME_MAP.put("Urban Planning / Architecture Approval", R.string.doc_arch_approval);
        DOC_NAME_MAP.put("Property Owner Agreement", R.string.doc_owner_agreement);
        DOC_NAME_MAP.put("Photo Simulation / Mockup", R.string.doc_photo_mockup);
        DOC_NAME_MAP.put("Demolition Technical Project", R.string.doc_demo_project);
        DOC_NAME_MAP.put("Demolition Plan", R.string.doc_demo_plan);
        DOC_NAME_MAP.put("Waste Management Study", R.string.doc_waste_study);
        DOC_NAME_MAP.put("Work Completion Inspection Report", R.string.doc_completion_report);
        DOC_NAME_MAP.put("Energy Performance Certificate", R.string.doc_energy_cert);
        DOC_NAME_MAP.put("Cadastral Documentation", R.string.doc_cadastral);
        DOC_NAME_MAP.put("Project Verifier Reports", R.string.doc_verifier_reports);
        DOC_NAME_MAP.put("Installation Compliance Declaration", R.string.doc_install_compliance);
    }

    public static String localizeType(Context ctx, String englishType) {
        if (englishType == null) return ctx.getString(R.string.unknown);
        Integer resId = PERMIT_TYPE_MAP.get(englishType);
        if (resId != null) return ctx.getString(resId);
        return englishType;
    }

    public static String localizeDoc(Context ctx, String englishDoc) {
        if (englishDoc == null) return "";
        Integer resId = DOC_NAME_MAP.get(englishDoc);
        if (resId != null) return ctx.getString(resId);
        return englishDoc;
    }

    public static String localizeStatus(Context ctx, String status) {
        if (status == null) return ctx.getString(R.string.unknown);
        switch (status) {
            case "submitted": return ctx.getString(R.string.filter_submitted);
            case "approved": return ctx.getString(R.string.filter_approved);
            case "rejected": return ctx.getString(R.string.filter_rejected);
            case "completed": return ctx.getString(R.string.filter_completed);
            default: return status.substring(0, 1).toUpperCase() + status.substring(1);
        }
    }

    public static String[] getLocalizedRequiredDocs(Context ctx, String permitTypeEnglish) {
        String[][] docKeys = getRequiredDocKeys(permitTypeEnglish);
        if (docKeys == null) return null;
        String[] result = new String[docKeys.length];
        for (int i = 0; i < docKeys.length; i++) {
            result[i] = localizeDoc(ctx, docKeys[i][0]);
        }
        return result;
    }

    public static String[][] getRequiredDocKeys(String permitTypeEnglish) {
        switch (permitTypeEnglish) {
            case "Construction Permit":
                return new String[][]{
                    {"Urban Planning Certificate"},
                    {"Land Registry Extract"},
                    {"Topographic Survey Plan"},
                    {"Authorized Technical Project"},
                    {"Utility Approvals (Water, Gas, Electricity)"},
                    {"Geotechnical Study"},
                    {"Fee Payment Proof"}
                };
            case "Renovation Permit":
                return new String[][]{
                    {"Urban Planning Certificate"},
                    {"Existing Condition Survey"},
                    {"Renovation Technical Project"},
                    {"Homeowners Association Approval (if applicable)"},
                    {"Affected Utility Approvals"},
                    {"Fee Payment Proof"}
                };
            case "Business License":
                return new String[][]{
                    {"Business Registration Certificate"},
                    {"Articles of Incorporation"},
                    {"Office Space Lease Agreement"},
                    {"Fire Safety Approval"},
                    {"Tax Clearance Certificate"},
                    {"Business Registry Certificate"}
                };
            case "Food Service Permit":
                return new String[][]{
                    {"Veterinary Sanitary Authorization"},
                    {"HACCP Plan"},
                    {"Pest Control Service Contract"},
                    {"Environmental Approval"},
                    {"Business Registration Certificate"},
                    {"Water Quality Analysis Report"}
                };
            case "Event Permit":
                return new String[][]{
                    {"Event Organization Request"},
                    {"Security Plan"},
                    {"Police Approval"},
                    {"Fire Department Approval"},
                    {"Sanitation Service Contract"},
                    {"Liability Insurance Policy"}
                };
            case "Signage Permit":
                return new String[][]{
                    {"Signage Placement Request"},
                    {"Site Sketch"},
                    {"Urban Planning / Architecture Approval"},
                    {"Property Owner Agreement"},
                    {"Photo Simulation / Mockup"}
                };
            case "Demolition Permit":
                return new String[][]{
                    {"Urban Planning Certificate"},
                    {"Land Registry Extract"},
                    {"Demolition Technical Project"},
                    {"Demolition Plan"},
                    {"Environmental Approval"},
                    {"Waste Management Study"},
                    {"Fee Payment Proof"}
                };
            case "Occupancy Certificate":
                return new String[][]{
                    {"Work Completion Inspection Report"},
                    {"Energy Performance Certificate"},
                    {"Cadastral Documentation"},
                    {"Project Verifier Reports"},
                    {"Installation Compliance Declaration"},
                    {"Fee Payment Proof"}
                };
            default:
                return null;
        }
    }
}

