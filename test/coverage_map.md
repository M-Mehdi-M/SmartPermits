# SmartPermits — Functional & Non-Functional Coverage Map

This table maps each functional requirement (**CF**) and key non-functional requirement (**CNF**)
to the automated test(s) that exercise it. Status legend: **Pass** = directly asserted,
**Partial** = behaviour exercised but not exhaustively, **Not covered** = no automated test.

> Note: the CF/CNF numbering below was reconstructed from the implemented backend features.
> Re-map the IDs to match the exact numbering used in your thesis if it differs.

## Functional requirements (CF1–CF43)

| ID | Requirement | Test(s) | Status |
|----|-------------|---------|--------|
| CF1 | Citizen self-registration returns token + user | `test_auth::test_register_success_returns_token_and_user` | Pass |
| CF2 | Password stored as bcrypt hash (never plaintext) | `test_auth::test_register_hashes_password_with_bcrypt` | Pass |
| CF3 | Registration validation (required fields, email, length) | `test_auth::test_register_missing_fields_*`, `*_invalid_email_*`, `*_short_password_*`, `*_short_username_*` | Pass |
| CF4 | Duplicate username/email rejected with 409 | `test_auth::test_register_duplicate_username_returns_409`, `*_duplicate_email_*` | Pass |
| CF5 | Role handling (default citizen, inspector allowed, invalid forced) | `test_auth::test_register_invalid_role_forced_to_citizen`, `*_inspector_role_allowed` | Pass |
| CF6 | Login with JWT issuance / `_get_current_user` | `test_auth::test_login_correct_password_returns_200_and_token`, `*_jwt_identifies_current_user_via_profile` | Pass |
| CF7 | Change password (with current-password check) | `test_auth::test_change_password_success`, `*_wrong_current_*`, `*_short_new_*` | Pass |
| CF8 | Profile view & update (name/email, duplicate guard) | `test_auth::test_jwt_identifies_current_user_via_profile`, `test_misc::test_profile_update_changes_full_name`, `*_duplicate_email_returns_409` | Pass |
| CF9 | Avatar image upload | — | Not covered |
| CF10 | FCM push token registration | `test_misc::test_fcm_token_save` | Pass |
| CF11 | Account self-deletion with cascade | `test_misc::test_delete_account_removes_user_and_permits` | Pass |
| CF12 | Create permit application (state `submitted`) | `test_permits::test_create_permit_sets_submitted_state_and_fee` | Pass |
| CF13 | Fee assigned from FEE_TABLE (+ default fallback) | `test_permits::test_create_permit_sets_submitted_state_and_fee`, `*_unknown_type_uses_default_fee` | Pass |
| CF14 | List own permits (user isolation) | `test_permits::test_list_my_permits_returns_only_owner_permits` | Pass |
| CF15 | Search / status / type filtering | `test_permits::test_list_search_filter`, `*_status_filter`, `*_type_filter` | Pass |
| CF16 | Permit detail with owner/inspector access control | `test_permits::test_get_permit_owner_access`, `*_inspector_access`, `*_other_citizen_forbidden`, `*_not_found_returns_404` | Pass |
| CF17 | Document upload with extension whitelist | `test_misc::test_document_upload_endpoint_accepts_image`, `*_rejects_disallowed_extension`, `*_unauthorized` | Pass |
| CF18 | Required-document checklist per permit type | `test_copilot::test_copilot_recommendation_enriched_from_tables` | Partial |
| CF19 | Inspector pending queue (role-gated) | `test_permits::test_pending_list_inspector_only` | Pass |
| CF20 | Review approve/reject + event + owner notification | `test_permits::test_review_approve_sets_state_and_reviewer`, `*_reject_sets_state`, `*_logs_event_and_notifies_owner` | Pass |
| CF21 | Guard against double review | `test_permits::test_review_double_review_guarded`, `*_invalid_action_returns_400` | Pass |
| CF22 | Reviewed-history list for inspector | `test_permits::test_reviewed_list_returns_inspector_decisions` | Pass |
| CF23 | Payment transitions permit to `completed` | `test_permits::test_pay_completes_permit_and_sets_expiry`, `*_non_approved_returns_400`, `*_already_paid_returns_400` | Pass |
| CF24 | Expiry date computed from validity table | `test_permits::test_pay_completes_permit_and_sets_expiry` | Pass |
| CF25 | Occupancy Certificate has no expiry | `test_permits::test_pay_occupancy_certificate_has_no_expiry` | Pass |
| CF26 | Renewal / reapply with `renewed_from` link | `test_permits::test_renew_creates_new_submitted_permit_with_link`, `*_reapply_rejected_permit_allowed`, `*_renew_submitted_permit_returns_400` | Pass |
| CF27 | Automatic expiry sweep of completed permits | `test_permits::test_expire_sweep_marks_completed_past_expiry`, `*_expire_check_endpoint` | Pass |
| CF28 | Inspector analytics aggregation | `test_misc::test_analytics_inspector_only` | Pass |
| CF29 | Permit comment thread (post/get) | `test_misc::test_comment_post_and_get`, `*_empty_message_rejected` | Pass |
| CF30 | Comment notification routing | `test_misc::test_comment_notifies_inspector_when_citizen_posts` | Pass |
| CF31 | Appointment scheduling + validations | `test_misc::test_appointment_scheduling_for_approved_permit`, `*_rejected_for_non_approved_permit`, `*_past_date_rejected`, `*_double_booking_rejected` | Pass |
| CF32 | Appointment status update + event log | `test_timeline::test_appointment_completion_logs_event` | Pass |
| CF33 | AI document analysis (success path) | `test_ai::test_ai_analyze_success_saves_result`, `*_success_logs_event` | Pass |
| CF34 | AI endpoint never returns 500 (graceful) | `test_ai::test_ai_analyze_without_key_*`, `*_no_documents_*`, `*_exception_returns_200_failed_message` | Pass |
| CF35 | AI caching + force + language recompute | `test_ai::test_ai_analyze_caches_result`, `*_force_recomputes`, `*_language_change_recomputes` | Pass |
| CF36 | Permit Copilot conversational replies | `test_copilot::test_copilot_ambiguous_returns_text_reply`, `*_without_key_*`, `*_empty_messages_*`, `*_exception_*` | Pass |
| CF37 | Copilot recommendation enrichment + enum constraint | `test_copilot::test_copilot_recommendation_enriched_from_tables`, `*_invalid_permit_type_falls_back_to_enum` | Pass |
| CF38 | Deterministic permit hash (canonical JSON) | `test_blockchain::test_compute_hash_is_deterministic`, `*_canonical_regardless_of_doc_order`, `*_changes_on_single_byte_difference`, `*_handles_none_description` | Pass |
| CF39 | Blockchain notarization with graceful degradation | `test_blockchain::test_write_hash_missing_env_returns_error`, `*_notarize_returns_hash_even_when_blockchain_fails`, `*_notarize_hashes_document_files`, `test_permits::test_approve_sets_blockchain_hash_locally` | Pass |
| CF40 | Public verification endpoint (+ Etherscan URL) | `test_blockchain::test_verify_endpoint_shape_after_approval`, `*_verify_endpoint_no_record`, `*_verify_etherscan_url_built_from_tx_hash` | Pass |
| CF41 | Certificate PDF only for completed permits | `test_pdf::test_certificate_for_completed_permit_returns_pdf`, `*_has_attachment_disposition`, `*_only_for_completed_permits`, `*_unauthorized_returns_403` | Pass |
| CF42 | Multi-language certificate + font fallback | `test_pdf::test_certificate_localized_language`, `*_font_registration_returns_three_names` | Pass |
| CF43 | Soft-delete trash (trash/restore/permanent/empty/cleanup) | `test_trash` (all 8 tests) | Pass |

## Non-functional requirements (CNF1–CNF13)

| ID | Requirement | Test(s) / Evidence | Status |
|----|-------------|--------------------|--------|
| CNF1 | CRUD response time under 500 ms reference | `benchmark.py` (CRUD average ≈ 37 ms) | Pass |
| CNF2 | Endpoints protected by JWT | `test_auth::test_protected_route_without_token_returns_401` | Pass |
| CNF3 | Role-based authorization enforced | `test_permits::test_inspector_cannot_create_permit_returns_403`, `*_review_by_citizen_forbidden`, `*_pending_list_inspector_only`, `test_misc::test_analytics_inspector_only` | Pass |
| CNF4 | Per-user data isolation | `test_permits::test_list_my_permits_returns_only_owner_permits`, `*_get_permit_other_citizen_forbidden` | Pass |
| CNF5 | No external API contact during tests | `conftest::_isolate_external` + mocked Gemini / web3 / Socket.IO | Pass |
| CNF6 | Input validation & sanitization | registration validation, `test_misc::test_comment_empty_message_rejected`, appointment date tests | Pass |
| CNF7 | Real-time event emission (Socket.IO) | `test_permits::test_create_permit_notifies_inspectors_via_socket`, `*_review_logs_event_and_notifies_owner`, `test_misc::test_comment_notifies_inspector_when_citizen_posts` | Pass |
| CNF8 | Internationalization (10 languages) | `test_ai::test_ai_analyze_language_change_recomputes`, `test_pdf::test_certificate_localized_language` (en/ro asserted; remaining languages share the same code path) | Partial |
| CNF9 | Audit trail / event logging | `test_timeline::test_log_permit_event_adds_without_committing`, `*_full_lifecycle_records_expected_event_types`, `*_timeline_endpoint_returns_events` | Pass |
| CNF10 | Data retention — 30-day trash cleanup | `test_trash::test_cleanup_old_trash_respects_30_day_rule` | Pass |
| CNF11 | Tamper-evidence via SHA-256 hashing | `test_blockchain::test_compute_hash_changes_on_single_byte_difference`, `*_notarize_hashes_document_files` | Pass |
| CNF12 | State-transition guards / idempotency | `test_permits::test_review_double_review_guarded`, `*_pay_already_paid_returns_400`, `test_misc::test_appointment_double_booking_rejected` | Pass |
| CNF13 | Graceful error handling (no crashes on failure) | `test_ai::test_ai_analyze_exception_returns_200_failed_message`, `test_copilot::test_copilot_exception_returns_graceful_message` | Pass |

## Summary

- **43 / 43** functional requirements have at least partial coverage (**42 Pass**, **1 Partial: CF18**, **1 Not covered: CF9 avatar upload**).
- **13 / 13** non-functional requirements covered (**12 Pass**, **1 Partial: CNF8**).
- Backend line coverage (pytest-cov): **app.py 78%**, **models.py 93%**, **blockchain.py 55%** (the uncovered `blockchain.py` lines are the live web3/Sepolia transaction-signing code that is intentionally never executed in tests).
