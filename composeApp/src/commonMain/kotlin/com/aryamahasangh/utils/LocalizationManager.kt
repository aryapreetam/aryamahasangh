package com.aryamahasangh.utils

/**
 * LocalizationManager handles message code to Hindi translation for CRUD function responses.
 *
 * This manager specifically handles message codes returned by our comprehensive Supabase functions:
 * - insertAryaSamajDetails
 * - updateAryaSamajDetails
 * - deleteAryaSamaj
 * - insertMemberDetails
 * - updateMemberDetails
 * - deleteMember
 * - insertFamilyDetails
 * - updateFamilyDetails
 * - deleteFamily
 */
object LocalizationManager {

  /**
   * Translates a message code to Hindi with Devanagari script.
   * Falls back to English if translation not found.
   */
  fun translateMessageCode(messageCode: String?): String {
    if (messageCode == null) return "कोई संदेश नहीं"

    return successMessages[messageCode]
      ?: errorMessages[messageCode]
      ?: messageCode // Fallback to original code if not found
  }

  /**
   * Checks if a message code represents a success response
   */
  fun isSuccessMessage(messageCode: String?): Boolean {
    return messageCode != null && successMessages.containsKey(messageCode)
  }

  /**
   * Checks if a message code represents an error response
   */
  fun isErrorMessage(messageCode: String?): Boolean {
    return messageCode != null && errorMessages.containsKey(messageCode)
  }

  /**
   * Gets a localized message with fallback to English
   */
  fun getLocalizedMessage(
    messageCode: String?,
    fallbackMessage: String? = null
  ): String {
    val translated = translateMessageCode(messageCode)

    // If translation is same as input (not found), use fallback
    return if (translated == messageCode && fallbackMessage != null) {
      fallbackMessage
    } else {
      translated
    }
  }

  // Success message translations in Hindi (Devanagari script)
  private val successMessages = mapOf(
    // Arya Samaj success messages
    "ARYA_SAMAJ_CREATED_SUCCESSFULLY" to "आर्य समाज सफलतापूर्वक बनाया गया",
    "ARYA_SAMAJ_UPDATED_SUCCESSFULLY" to "आर्य समाज की जानकारी सफलतापूर्वक अपडेट की गई",
    "ARYA_SAMAJ_DELETED_SUCCESSFULLY" to "आर्य समाज सफलतापूर्वक हटाया गया",

    // Member success messages
    "MEMBER_CREATED_SUCCESSFULLY" to "सदस्य सफलतापूर्वक जोड़ा गया",
    "MEMBER_UPDATED_SUCCESSFULLY" to "सदस्य की जानकारी सफलतापूर्वक अपडेट की गई",
    "MEMBER_DELETED_SUCCESSFULLY" to "सदस्य सफलतापूर्वक हटाया गया",

    // Family success messages
    "FAMILY_CREATED_SUCCESSFULLY" to "परिवार सफलतापूर्वक बनाया गया",
    "FAMILY_UPDATED_SUCCESSFULLY" to "परिवार की जानकारी सफलतापूर्वक अपडेट की गई",
    "FAMILY_DELETED_SUCCESSFULLY" to "परिवार सफलतापूर्वक हटाया गया",

    // Address success messages
    "ADDRESS_CREATED_SUCCESSFULLY" to "पता सफलतापूर्वक जोड़ा गया",
    "ADDRESS_UPDATED_SUCCESSFULLY" to "पता सफलतापूर्वक अपडेट किया गया",

    // Relationship success messages
    "MEMBER_ADDED_TO_FAMILY" to "सदस्य को परिवार में सफलतापूर्वक जोड़ा गया",
    "MEMBER_REMOVED_FROM_FAMILY" to "सदस्य को परिवार से सफलतापूर्वक हटाया गया",
    "MEMBER_ADDED_TO_ARYA_SAMAJ" to "सदस्य को आर्य समाज में सफलतापूर्वक जोड़ा गया",
    "MEMBER_REMOVED_FROM_ARYA_SAMAJ" to "सदस्य को आर्य समाज से सफलतापूर्वक हटाया गया",

    // General success messages
    "OPERATION_COMPLETED_SUCCESSFULLY" to "कार्य सफलतापूर्वक पूरा हुआ",
    "RECORD_CREATED_SUCCESSFULLY" to "रिकॉर्ड सफलतापूर्वक बनाया गया",
    "RECORD_UPDATED_SUCCESSFULLY" to "रिकॉर्ड सफलतापूर्वक अपडेट किया गया",
    "RECORD_DELETED_SUCCESSFULLY" to "रिकॉर्ड सफलतापूर्वक हटाया गया"
  )

  // Error message translations in Hindi (Devanagari script)
  private val errorMessages = mapOf(
    // Not found errors
    "ARYA_SAMAJ_NOT_FOUND" to "आर्य समाज नहीं मिला",
    "MEMBER_NOT_FOUND" to "सदस्य नहीं मिला",
    "FAMILY_NOT_FOUND" to "परिवार नहीं मिला",
    "ADDRESS_NOT_FOUND" to "पता नहीं मिला",
    "RECORD_NOT_FOUND" to "रिकॉर्ड नहीं मिला",

    // Creation errors
    "ERROR_CREATING_ARYA_SAMAJ" to "आर्य समाज बनाने में त्रुटि",
    "ERROR_CREATING_MEMBER" to "सदस्य जोड़ने में त्रुटि",
    "ERROR_CREATING_FAMILY" to "परिवार बनाने में त्रुटि",
    "ERROR_CREATING_ADDRESS" to "पता जोड़ने में त्रुटि",
    "ERROR_CREATING_RECORD" to "रिकॉर्ड बनाने में त्रुटि",

    // Update errors
    "ERROR_UPDATING_ARYA_SAMAJ" to "आर्य समाज अपडेट करने में त्रुटि",
    "ERROR_UPDATING_MEMBER" to "सदस्य की जानकारी अपडेट करने में त्रुटि",
    "ERROR_UPDATING_FAMILY" to "परिवार की जानकारी अपडेट करने में त्रुटि",
    "ERROR_UPDATING_ADDRESS" to "पता अपडेट करने में त्रुटि",
    "ERROR_UPDATING_RECORD" to "रिकॉर्ड अपडेट करने में त्रुटि",

    // Deletion errors
    "ERROR_DELETING_ARYA_SAMAJ" to "आर्य समाज हटाने में त्रुटि",
    "ERROR_DELETING_MEMBER" to "सदस्य हटाने में त्रुटि",
    "ERROR_DELETING_FAMILY" to "परिवार हटाने में त्रुटि",
    "ERROR_DELETING_ADDRESS" to "पता हटाने में त्रुटि",
    "ERROR_DELETING_RECORD" to "रिकॉर्ड हटाने में त्रुटि",

    // Validation errors
    "INVALID_NAME" to "अमान्य नाम",
    "INVALID_PHONE_NUMBER" to "अमान्य फोन नंबर",
    "INVALID_EMAIL" to "अमान्य ईमेल पता",
    "INVALID_DATE" to "अमान्य तारीख",
    "INVALID_ADDRESS" to "अमान्य पता",
    "REQUIRED_FIELD_MISSING" to "आवश्यक जानकारी गुम है",
    "NAME_REQUIRED" to "नाम आवश्यक है",
    "PHONE_NUMBER_REQUIRED" to "फोन नंबर आवश्यक है",
    "ADDRESS_REQUIRED" to "पता आवश्यक है",
    "FAMILY_NAME_REQUIRED" to "परिवार का नाम आवश्यक है",
    "HEAD_MEMBER_REQUIRED" to "प्रमुख सदस्य आवश्यक है",

    // Business logic errors
    "MEMBER_ALREADY_IN_FAMILY" to "सदस्य पहले से ही परिवार में है",
    "MEMBER_ALREADY_IN_ARYA_SAMAJ" to "सदस्य पहले से ही आर्य समाज में है",
    "CANNOT_DELETE_FAMILY_WITH_MEMBERS" to "सदस्यों वाले परिवार को नहीं हटाया जा सकता",
    "CANNOT_DELETE_ARYA_SAMAJ_WITH_MEMBERS" to "सदस्यों वाले आर्य समाज को नहीं हटाया जा सकता",
    "DUPLICATE_PHONE_NUMBER" to "यह फोन नंबर पहले से मौजूद है",
    "DUPLICATE_EMAIL" to "यह ईमेल पता पहले से मौजूद है",
    "HEAD_MEMBER_NOT_IN_FAMILY" to "प्रमुख सदस्य परिवार में नहीं है",

    // Permission errors
    "INSUFFICIENT_PERMISSIONS" to "अपर्याप्त अनुमतियां",
    "UNAUTHORIZED_ACCESS" to "अनधिकृत पहुंच",
    "OPERATION_NOT_ALLOWED" to "यह कार्य अनुमतित नहीं है",

    // System errors
    "DATABASE_ERROR" to "डेटाबेस त्रुटि",
    "SERVER_ERROR" to "सर्वर त्रुटि",
    "NETWORK_ERROR" to "नेटवर्क त्रुटि",
    "INTERNAL_ERROR" to "आंतरिक त्रुटि",
    "UNKNOWN_ERROR" to "अज्ञात त्रुटि",
    "OPERATION_FAILED" to "कार्य असफल",

    // Connection errors
    "CONNECTION_FAILED" to "कनेक्शन असफल",
    "TIMEOUT_ERROR" to "समय सीमा समाप्त",
    "REQUEST_FAILED" to "अनुरोध असफल"
  )
}
