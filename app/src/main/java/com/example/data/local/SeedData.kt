package com.example.data.local

import com.example.data.model.ServiceCategory

object SeedData {
    const val ADMIN_EMAIL = "m95169829@gmail.com"

    fun baseServices(): List<ServiceEntity> = listOf(
        ServiceEntity(
            id = "srv_robux",
            category = ServiceCategory.ROBUX.key,
            name = "شحن Robux",
            description = "بطاقات ورصيد Robux أصلية لحسابات روبلوكس",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 1
        ),
        ServiceEntity(
            id = "srv_freefire",
            category = ServiceCategory.FREE_FIRE.key,
            name = "شحن Free Fire (جواهر)",
            description = "شحن مباشر عبر Player ID بدون كلمة سر",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 2
        ),
        ServiceEntity(
            id = "srv_pubg",
            category = ServiceCategory.PUBG.key,
            name = "شحن PUBG (UC)",
            description = "شحن شدات UC بالـ ID لحسابات PUBG",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 3
        ),
        ServiceEntity(
            id = "srv_steam",
            category = ServiceCategory.STEAM.key,
            name = "Steam (بطاقات ومحفظة)",
            description = "أكواد شحن محفظة ستيم الرقمية",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 4
        ),
        ServiceEntity(
            id = "srv_psn",
            category = ServiceCategory.PLAYSTATION.key,
            name = "بطاقات PlayStation",
            description = "بطاقات بلايستيشن ستور الرقمية",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 5
        ),
        ServiceEntity(
            id = "srv_xbox",
            category = ServiceCategory.XBOX.key,
            name = "بطاقات Xbox",
            description = "اشتراكات Game Pass وبطاقات رصيد Xbox",
            packagesJson = "[]",
            isAvailable = true,
            displayOrder = 6
        )
    )

    suspend fun seedIfEmpty(db: TwinCashDatabase) {
        val isFirstLaunch = db.settingsDao().getSettings() == null
        if (!isFirstLaunch) return

        db.settingsDao().upsertSettings(
            SettingsEntity(
                id = "global",
                agencyPhone = "0670533324",
                whatsappNumber = "212670533324",
                loyaltyThreshold = 4,
                loyaltyDiscountPercent = 25.0,
                adminEmail = ADMIN_EMAIL,
                agencyBalance = 0.0
            )
        )

        if (db.serviceDao().countServices() == 0) {
            db.serviceDao().insertAll(baseServices())
        }
    }
}
