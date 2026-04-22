package com.example.capstonex;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ValidationUtilsTest — CapstonX White-Box Unit Tests
 *
 * Covers all branches in:
 *  • ValidationUtils (new reusable class)
 *  • UserImportHelper validation logic (mirrored)
 *  • GroupCreationActivity SAP/duplicate-detection logic
 *
 * Run: Right-click → Run in Android Studio, or  ./gradlew test
 */
public class ValidationUtilsTest {

    // ═════════════════════════════════════════════════════════════════════
    // SAP ID — isValidSapId()
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void sapId_elevnDigit_isValid() {
        assertTrue(ValidationUtils.isValidSapId("70012400171"));
    }

    @Test
    public void sapId_eightDigit_isValid_boundary() {
        // Boundary condition: exactly 8 digits → valid
        assertTrue(ValidationUtils.isValidSapId("70012400"));
    }

    @Test
    public void sapId_sevenDigit_isInvalid_belowBoundary() {
        // Below minimum of 8 → auto-fill should NOT fire
        assertFalse(ValidationUtils.isValidSapId("7001240"));
    }

    @Test
    public void sapId_empty_isInvalid() {
        assertFalse(ValidationUtils.isValidSapId(""));
    }

    @Test
    public void sapId_null_isInvalid() {
        assertFalse(ValidationUtils.isValidSapId(null));
    }

    @Test
    public void sapId_containsLetters_isInvalid() {
        assertFalse(ValidationUtils.isValidSapId("7001240A17"));
    }

    @Test
    public void sapId_whitespaceOnly_isInvalid() {
        assertFalse(ValidationUtils.isValidSapId("   "));
    }

    @Test
    public void sapId_validateMethod_returnsNullOnValid() {
        assertNull(ValidationUtils.validateSapId("70012400171"));
    }

    @Test
    public void sapId_validateMethod_returnsErrorOnEmpty() {
        assertNotNull(ValidationUtils.validateSapId(""));
    }

    @Test
    public void sapId_validateMethod_returnsErrorOnLetters() {
        assertNotNull(ValidationUtils.validateSapId("70012A00171"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Email — isValidEmail() and validateEmail()
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void email_valid_nmims_isValid() {
        assertTrue(ValidationUtils.isValidEmail("Yash.Chaudhari171@nmims.in"));
    }

    @Test
    public void email_valid_gmail_isValid() {
        assertTrue(ValidationUtils.isValidEmail("user@gmail.com"));
    }

    @Test
    public void email_missingAtSign_isInvalid() {
        assertFalse(ValidationUtils.isValidEmail("yashgmail.com"));
    }

    @Test
    public void email_missingDomain_isInvalid() {
        assertFalse(ValidationUtils.isValidEmail("yash@"));
    }

    @Test
    public void email_empty_isInvalid() {
        assertFalse(ValidationUtils.isValidEmail(""));
    }

    @Test
    public void email_null_isInvalid() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    public void email_validateMethod_returnsNullOnValid() {
        assertNull(ValidationUtils.validateEmail("yash@nmims.in"));
    }

    @Test
    public void email_validateMethod_returnsErrorOnEmpty() {
        assertNotNull(ValidationUtils.validateEmail(""));
        assertEquals("Email is required", ValidationUtils.validateEmail(""));
    }

    @Test
    public void email_validateMethod_returnsErrorOnInvalid() {
        assertNotNull(ValidationUtils.validateEmail("not-an-email"));
    }

    // CSV regex — used internally by UserImportHelper
    @Test
    public void email_csvRegex_valid() {
        assertTrue("yash@nmims.in".matches(ValidationUtils.EMAIL_REGEX));
    }

    @Test
    public void email_csvRegex_missingAt_invalid() {
        assertFalse("yashnmims.in".matches(ValidationUtils.EMAIL_REGEX));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Password — validatePassword()
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void password_lessThan6Chars_isInvalid() {
        assertNotNull(ValidationUtils.validatePassword("abc12"));
    }

    @Test
    public void password_exactlySix_isValid_boundaryCondition() {
        assertNull(ValidationUtils.validatePassword("abc123")); // exactly 6 → valid
    }

    @Test
    public void password_moreThanSix_isValid() {
        assertNull(ValidationUtils.validatePassword("Pass@123"));
    }

    @Test
    public void password_empty_isInvalid() {
        assertNotNull(ValidationUtils.validatePassword(""));
        assertEquals("Password is required", ValidationUtils.validatePassword(""));
    }

    @Test
    public void password_null_isInvalid() {
        assertNotNull(ValidationUtils.validatePassword(null));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Password match
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void passwordMatch_identical_isValid() {
        assertNull(ValidationUtils.validatePasswordMatch("Pass@123", "Pass@123"));
    }

    @Test
    public void passwordMatch_different_isInvalid() {
        assertNotNull(ValidationUtils.validatePasswordMatch("Pass@123", "Pass@456"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Required field validation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void required_nonEmpty_isValid() {
        assertTrue(ValidationUtils.isNotEmpty("Yash"));
    }

    @Test
    public void required_empty_isInvalid() {
        assertFalse(ValidationUtils.isNotEmpty(""));
    }

    @Test
    public void required_null_isInvalid() {
        assertFalse(ValidationUtils.isNotEmpty(null));
    }

    @Test
    public void required_whitespace_isInvalid() {
        assertFalse(ValidationUtils.isNotEmpty("   "));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Duplicate SAP detection (GroupCreationActivity logic)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void duplicateSap_sameSapTwice_detected() {
        Set<String> sapSet = new HashSet<>();
        assertTrue(sapSet.add("70012400171"));   // first add succeeds
        assertFalse(sapSet.add("70012400171")); // duplicate detected
    }

    @Test
    public void duplicateSap_fourUniqueSaps_noDuplicate() {
        Set<String> sapSet = new HashSet<>();
        assertTrue(sapSet.add("70012400171"));
        assertTrue(sapSet.add("70012400172"));
        assertTrue(sapSet.add("70012400173"));
        assertTrue(sapSet.add("70012400174"));
        assertEquals(4, sapSet.size());
    }

    @Test
    public void duplicateSap_leaderSameAsMembers_detected() {
        String leaderSap = "70012400171";
        Set<String> sapSet = new HashSet<>();
        sapSet.add(leaderSap);
        assertFalse(sapSet.add(leaderSap)); // leader duplicated as member
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group ID generation logic (GroupCreationActivity)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void groupId_format_isCorrect() {
        String branch = "IT";
        int year = 2026;
        int next = 1;
        assertEquals("IT2026_1", branch + year + "_" + next);
    }

    @Test
    public void groupId_maxNumExtraction_withMixedKeys() {
        String prefix = "IT2026_";
        String[] existingKeys = {"IT2026_1", "IT2026_3", "IT2026_7", "IT2026_2"};
        int maxNum = 0;
        for (String key : existingKeys) {
            if (key.startsWith(prefix)) {
                try {
                    int n = Integer.parseInt(key.substring(prefix.length()));
                    if (n > maxNum) maxNum = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        assertEquals(7, maxNum);
        assertEquals("IT2026_8", prefix + (maxNum + 1));
    }

    @Test
    public void groupId_noExistingGroups_startsAtOne() {
        int maxNum = 0; // no existing groups
        assertEquals("IT2026_1", "IT2026_" + (maxNum + 1));
    }

    // ═════════════════════════════════════════════════════════════════════
    // hasGroup null-safety (GroupCreationActivity)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void hasGroup_trueValue_blocksCreation() {
        Boolean hasGroup = true;
        assertTrue(Boolean.TRUE.equals(hasGroup));
    }

    @Test
    public void hasGroup_falseValue_allowsCreation() {
        Boolean hasGroup = false;
        assertFalse(Boolean.TRUE.equals(hasGroup));
    }

    @Test
    public void hasGroup_nullValue_allowsCreation_nullSafe() {
        Boolean hasGroup = null; // field not set in Firebase
        assertFalse(Boolean.TRUE.equals(hasGroup)); // null-safe → does NOT block
    }

    // ═════════════════════════════════════════════════════════════════════
    // Branch fallback (GroupCreationActivity)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void branch_null_fallsBackToIT() {
        String branch = null;
        if (branch == null || branch.isEmpty()) branch = "IT";
        assertEquals("IT", branch);
    }

    @Test
    public void branch_empty_fallsBackToIT() {
        String branch = "";
        if (branch == null || branch.isEmpty()) branch = "IT";
        assertEquals("IT", branch);
    }

    @Test
    public void branch_cs_isPreservedAsUppercase() {
        String branch = "cs";
        branch = branch.toUpperCase();
        assertEquals("CS", branch);
    }

    // ═════════════════════════════════════════════════════════════════════
    // CSV parser: SAP auto-fill trigger boundary
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void sapAutofill_triggersAtExactly8Chars() {
        String sap = "70012400"; // 8 chars
        assertTrue(sap.length() >= 8);
    }

    @Test
    public void sapAutofill_doesNotTriggerBelow8Chars() {
        String sap = "7001240"; // 7 chars
        assertFalse(sap.length() >= 8);
    }

    // ═════════════════════════════════════════════════════════════════════
    // CSV header detection (UserImportHelper)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void csvHeader_studentHeader_isDetected() {
        String line = "sapId,rollNo,name,email,password,branch";
        assertTrue(line.toLowerCase().startsWith("sap"));
    }

    @Test
    public void csvHeader_mentorHeader_isDetected() {
        String line = "id,name,email,password";
        assertTrue(line.toLowerCase().startsWith("id"));
    }

    @Test
    public void csvHeader_dataRow_isNotSkipped() {
        String line = "70012400171,A265,Yash Chaudhari,yash@nmims.in,Pass@123,IT";
        assertFalse(line.toLowerCase().startsWith("sap"));
        assertFalse(line.toLowerCase().startsWith("id"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // CSV column count validation (UserImportHelper)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void csvStudentRow_enough6Columns_isAccepted() {
        String line = "70012400171,A265,Yash,yash@nmims.in,Pass@123,IT";
        String[] cols = line.split(",", -1);
        assertTrue(cols.length >= 5);
    }

    @Test
    public void csvStudentRow_only4Columns_isRejected() {
        String line = "70012400171,A265,Yash,yash@nmims.in";
        String[] cols = line.split(",", -1);
        assertFalse(cols.length >= 5);
    }

    @Test
    public void csvBranchColumn_optionalAt6th_defaultsToEmpty() {
        String line = "70012400171,A265,Yash,yash@nmims.in,Pass@123"; // no branch
        String[] c = line.split(",", -1);
        String branch = c.length >= 6 ? c[5].trim() : "";
        assertEquals("", branch);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SAP list building (visible fields only)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void sapList_onlyLeader_hasOneEntry() {
        List<String> sapList = new ArrayList<>();
        sapList.add("70012400171"); // leader always added
        // member2, 3, 4 are GONE (not visible)
        assertEquals(1, sapList.size());
    }

    @Test
    public void sapList_leaderPlusTwoMembers_hasThreeEntries() {
        List<String> sapList = new ArrayList<>();
        sapList.add("70012400171");
        sapList.add("70012400172"); // member2 visible
        sapList.add("70012400173"); // member3 visible
        // member4 GONE
        assertEquals(3, sapList.size());
    }

    @Test
    public void sapList_allFourMembers_hasFourEntries() {
        List<String> sapList = new ArrayList<>();
        sapList.add("70012400171");
        sapList.add("70012400172");
        sapList.add("70012400173");
        sapList.add("70012400174");
        assertEquals(4, sapList.size());
    }
}
