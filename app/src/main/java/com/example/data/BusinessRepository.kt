package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BusinessRepository(private val database: AppDatabase) {

    private val businessInfoDao = database.businessInfoDao()
    private val chatMessageDao = database.chatMessageDao()
    private val orderTicketDao = database.orderTicketDao()

    val businessInfo: Flow<BusinessInfo?> = businessInfoDao.getBusinessInfoFlow()
    val chatMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessagesFlow()
    val orderTickets: Flow<List<OrderTicket>> = orderTicketDao.getAllOrdersFlow()

    suspend fun getBusinessInfoDirect(): BusinessInfo? = businessInfoDao.getBusinessInfo()

    suspend fun updateBusiness(info: BusinessInfo) {
        businessInfoDao.insertOrUpdateBusiness(info)
    }

    suspend fun addMessage(sender: String, message: String) {
        chatMessageDao.insertMessage(ChatMessage(sender = sender, message = message))
    }

    suspend fun clearChat() {
        chatMessageDao.clearChat()
    }

    suspend fun addOrder(order: OrderTicket) {
        orderTicketDao.insertOrder(order)
    }

    suspend fun updateOrderStatus(id: Int, status: String) {
        orderTicketDao.updateOrderStatus(id, status)
    }

    suspend fun deleteOrder(id: Int) {
        orderTicketDao.deleteOrder(id)
    }

    suspend fun initializeWithDefaultIfNeeded() {
        val current = businessInfoDao.getBusinessInfo()
        if (current == null) {
            // Seed with first template (Kafé)
            applyTemplate(getTemplates()[0])
        }
    }

    suspend fun applyTemplate(template: BusinessTemplate) {
        val business = BusinessInfo(
            id = 1,
            name = template.name,
            type = template.type,
            systemPrompt = template.systemPrompt,
            welcomeMessage = template.welcomeMessage,
            productsJson = template.productsJson
        )
        updateBusiness(business)
        clearChat()
        // Add default assistant welcome message
        addMessage("ai", template.welcomeMessage)
    }

    data class BusinessTemplate(
        val name: String,
        val type: String,
        val systemPrompt: String,
        val welcomeMessage: String,
        val productsJson: String
    )

    fun getTemplates(): List<BusinessTemplate> {
        return listOf(
            BusinessTemplate(
                name = "Rayhon Milliy Taomlar",
                type = "Kafé/Restoran",
                systemPrompt = """Siz 'Rayhon Milliy Taomlar' milliy taomlar kafesining mehmondo'st, o'ta muloyim virtual yordamchisisiz. Vazifangiz: mijozlardan buyurtma olish yoki stol band qilish. 
Mijozning ismini, telefon raqamini, yetkazib berish/kelish sanasi va vaqtini, hamda buyurtma qiladigan taomlarini birmabir so'rang. 
Menyuda bor taomlarni tavsiya qiling. Mijoz hamma buyurtmani aytib, aniq tasdiqlaganda va telefon raqami hamda vaqtini taqdim etgandagina, quyidagi mahsulot ma'lumotlarini o'z ichiga olgan maxsus JSON buyurtma chiptasini javobigizning MUTLAQO OXIRIDA qo'shib yuborishingiz shart. JSON formatida hech qanday ortiqcha matn yoki izoh bo'lmasligi kerak:

[ORDER_TICKET:{"name":"Mijoz Ismi", "phone":"Mijoz telefoni", "datetime":"Sana va vaqt", "details":"Xarid qilingan taomlar va miqdori"}]

Mijoz salomlashsa yoki umumiy so'rasa, bot kabi emas, ochiq chehra bilan gaplashing. Har doim o'zbek tilida muloqot qiling.""",
                welcomeMessage = "Assalomu alaykum! 'Rayhon Milliy Taomlar' milliy taomlar maskaniga xush kelibsiz! 😊 Menga ismingizni aytsangiz, buyurtma qabul qilish yoki stol band qilishga yordam beraman. Bugun menyuimizda issiqqina To'y Palovi va mazali go'shtli Shashliklarimiz bor!",
                productsJson = """[
  {"name": "To'y Palovi (Sersobda)", "price": "45 000 UZS", "desc": "Zafarshon guruchi, mayda kishmish va sifatli mol go'shti bilan"},
  {"name": "Mol go'shtidan Shashlik", "price": "15 000 UZS", "desc": "Ko'mirda pishirilgan yumshoqqina lahm go'shti"},
  {"name": "Qovurma Lag'mon", "price": "35 000 UZS", "desc": "Cho'zma xamir, yangi uzilgan sabzavotlar va ziravorlar bilan"},
  {"name": "Tandir Somsa", "price": "8 000 UZS", "desc": "Tandirdan uzilgan sergo'sht va mayda somsa"},
  {"name": "Coca-Cola 1.5L", "price": "12 000 UZS", "desc": "Muzdek salqin ichimlik"}
]"""
            ),
            BusinessTemplate(
                name = "Lola Beauty",
                type = "Go'zallik saloni",
                systemPrompt = """Siz 'Lola Beauty' professional go'zallik salonining hushmuomala virtual administratorisiz. Vazifangiz: mijozlarni qabul qilish va xizmatlarga yozish. 
Mijozning ismi, telefon raqami, kerakli xizmat turi, master (mutaxassis) nomi hamda keladigan sana va vaqtini aniqlashtiring.
Mijoz xizmatlarni tanlab, telefon raqami va vaqtini aniq aytgandagina (va yozilishni tasdiqlaganda), quyidagi formatda buyurtma chiptasini javobigizning MUTLAQO OXIRIDA qo'shib yuborishingiz shart (chiptadan keyin hech narsa yozmang):

[ORDER_TICKET:{"name":"Mijoz Ismi", "phone":"Mijoz telefoni", "datetime":"Sana va vaqt", "details":"Xizmat turi va mutaxassis"}]

Muloqot har doim xushmuomala, zamonaviy va o'zbek tilida bo'lishi lozim.""",
                welcomeMessage = "Sizni kutib olganimizdan xursandmiz! 'Lola Beauty' salonimizga xush kelibsiz! 🌸 Soch turmaklash, go'zal makiyaj yoki sifatli manikyur uchun yozilib qo'yaymi? Qaysi xizmat qiziqtirmoqda?",
                productsJson = """[
  {"name": "Soch kesish va fason berish", "price": "50 000 UZS", "desc": "Tajribali soch ustasi tomonidan zamonaviy turmaklar"},
  {"name": "Professional Kechki Makiyaj", "price": "120 000 UZS", "desc": "Sifatli kosmetika vositalari bilan professional makiyaj"},
  {"name": "Apparatli Manikyur + Gel-lak", "price": "70 000 UZS", "desc": "Mukammal tirnoq parvarishi va istalgan dizaynlar"},
  {"name": "Kiprik ekish (Lash expansion)", "price": "90 000 UZS", "desc": "Tabiiy va jozibali ko'rinish beruvchi klassik kipriklar"}
]"""
            ),
            BusinessTemplate(
                name = "Avto-Mo'jiza Servis",
                type = "Avtoservis",
                systemPrompt = """Siz 'Avto-Mo'jiza Servis' avtomobillarga texnik xizmat ko'rsatsh markazining master-administratorisiz. Vazifangiz: mijozlar mashinasini ta'mirlash, moy almashtirish yoki yuvishga navbatga yozib qo'yish.
Mijozning ismi, mashina markasi va modeli, telefoni, kerakli xizmat turi hamda usta qabuliga keladigan sana va vaqtini aniqlashtiring.
Mijoz barcha ma'lumotlarni aytib, o'z navbatini tasdiqlaganida, javobning MUTLAQO OXIRIDA quyidagi JSON formatda buyurtma chiptasini qaytaring:

[ORDER_TICKET:{"name":"Mijoz Ismi", "phone":"Mijoz telefoni", "datetime":"Kelish vaqti", "details":"Xizmat turi va mashina markasi"}]

Sohaga oid professional va ishonchli gapiring. Har doim o'zbek tilida javob yo'llang.""",
                welcomeMessage = "Assalomu alaykum! 'Avto-Mo'jiza Servis' texnik xizmat ko'rsatish markaziga xush kelibsiz! 🚗 Mashinangizni usta ko'rigiga, moy almashtirishga yoki yuvishga navbatga yozib qo'ya olaman. Ismingizni va mashina modelingizni aytsangiz!",
                productsJson = """[
  {"name": "Kuzov va Salon to'liq yuvish", "price": "60 000 UZS", "desc": "Sifatli penali tozalash va salonni maxsus hid beruvchi bilan qoplash"},
  {"name": "Dvigatel moyini almashtirish", "price": "150 000 UZS", "desc": "Sintetika moylari, filterlar bepul qo'yib beriladi"},
  {"name": "Balansirovka va g'ildiraklar razvali", "price": "70 000 UZS", "desc": "Kompyuter yordamida aniq 3D muvozanatlash xizmati"},
  {"name": "Kompyuter Diagnostikasi", "price": "50 000 UZS", "desc": "Barcha datchiklar va xatoliklarni tekshirish (OBD2)"}
]"""
            ),
            BusinessTemplate(
                name = "Trend Wear do'koni",
                type = "Kiyim do'koni",
                systemPrompt = """Siz 'Trend Wear' zamonaviy kiyim-kechak brendi telegram-yordamchisisiz. Vazifangiz: mijozlardan kiyimlarga buyurtma olish va kuryer orqali yetkazish uchun ma'lumot to'plash.
Mijozga kiyim o'lchamlari va ranglarni tanlashda ko'maklashing. Buyurtmani rasmiylashtirish uchun: ism, telefon raqami, kiyim nomi va o'lchami, hamda yetkazib berish manzilini oling.
Mijoz barcha ma'lumotlarni berib, xaridni tasdiqlaganda, javobgizning MUTLAQO OXIRIDA quyidagi buyurtma chiptasini qo'shing:

[ORDER_TICKET:{"name":"Mijoz Ismi", "phone":"Mijoz telefoni", "datetime":"Kuyer orqali yetkazish", "details":"Kiyim nomi, o'lchami va manzili"}]

Brendingiz kabi hushmuomala va modabop gapiring. Har doim o'zbek tilida gapiring.""",
                welcomeMessage = "Sizga salom! 'Trend Wear' do'koniga xush kelibsiz! 🧥 Zamonaviy oversize futbolkalarimiz, klassik jinsilarimiz va trenddagi kurtkalarimiz kelgan! Buyurtma berish uchun sizga qaysi kiyim va qaysi o'lcham kerakligini ayting, yordam beraman!",
                productsJson = """[
  {"name": "Oversize Premium Futbolka", "price": "99 000 UZS", "desc": "100% paxtali mato, S, M, L o'lchamlar mavjud, oq va qora rangda"},
  {"name": "Klassik Jinsi shimlar (Yevropa)", "price": "189 000 UZS", "desc": "Sifatli va cho'ziluvchan, moviy hamda to'q ko'k ranglarda"},
  {"name": "Yengil Retro Charm Kurtka", "price": "349 000 UZS", "desc": "Kuzgi va bahor his etadigan qora sifatli eko-charm kurtka"},
  {"name": "Yozgi Breathy krossovkalar", "price": "250 000 UZS", "desc": "Yugurish va kundalik sayr uchun mos havo o'tkazadigan krossovkalar"}
]"""
            )
        )
    }
}
