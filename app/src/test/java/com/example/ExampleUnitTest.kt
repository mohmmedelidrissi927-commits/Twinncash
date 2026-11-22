package com.example

import com.example.data.local.PasswordHasher
import com.example.data.local.SeedData
import com.example.data.model.ContestPrizeType
import com.example.data.model.ContestStatus
import com.example.data.model.DiscountType
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun passwordHasher_verifiesCorrectAndRejectsWrong() {
        val hash = PasswordHasher.hashPassword("TwinCash#2026")
        assertTrue(PasswordHasher.verifyPassword("TwinCash#2026", hash))
        assertFalse(PasswordHasher.verifyPassword("wrong", hash))
    }

    @Test
    fun domainEnums_mapKeysCorrectly() {
        assertEquals(UserRole.ADMIN, UserRole.fromKey("admin"))
        assertEquals(UserRole.MERCHANT, UserRole.fromKey("merchant"))
        assertEquals(UserRole.CUSTOMER, UserRole.fromKey("unknown"))
        assertEquals(ServiceCategory.FREE_FIRE, ServiceCategory.fromKey("FREE_FIRE"))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromKey("completed"))
        assertEquals(DiscountType.FIXED, DiscountType.fromKey("fixed"))
        assertEquals(ContestPrizeType.DISCOUNT_CODE, ContestPrizeType.fromKey("discount_code"))
        assertEquals(ContestStatus.WINNER_SELECTED, ContestStatus.fromKey("winner_selected"))
    }

    @Test
    fun seedData_usesProductionAdminEmail() {
        assertEquals("m95169829@gmail.com", SeedData.ADMIN_EMAIL)
    }
}
