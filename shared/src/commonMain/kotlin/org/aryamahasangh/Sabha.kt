package org.aryamahasangh

data class Sabha(
  val name: String,
  val logo: String,
  val description: String,
  val keyPeople: List<OrganisationalMember> = listOf(),
  val activities: List<OrganisationActivity> = listOf(),
)

sealed class OrganisationActivity {
  /**
   * Karyakram:
   */
  data class Event(val name: String): OrganisationActivity()

  /**
   * Satr:
   */
  data class Session(val name: String): OrganisationActivity()

  /**
   * Abhiyan:
   */
  data class Campaign(val name: String): OrganisationActivity()
  data class Misc(val description: String): OrganisationActivity()
}


data class OrganisationalMember(
  val member: Member,
  val post: String,
  val priority: Int
)

data class Member(
  val name: String,
  val educationalQualification: String? = "",
  val profileImage: String?,
  val phoneNumber: String? = "",
  val email: String? = ""
)

val listOfSabha = listOf(
  Sabha(
    name = "राष्ट्रीय आर्य निर्मात्री सभा",
    logo = "nirmatri_sabha",
    description = "  प्रत्येक बुद्धिमान व्यक्ति यह समझ सकता है कि मानवीय जीवन अति दुर्लभ है। " +
            "लेकिन विडम्बना है कि इसके प्रयोजन को ढूंढने पर कोई विरला ही विचार करता है। " +
            "जीवन की सफलता के लिए आवश्यक है कि हम जानें कि इससे हम क्या प्राप्त कर सकते हैं और कैसे? " +
            "वास्तव में इन प्रश्नों के उत्तर हेतु धर्म के सत्य स्वरूप का ज्ञान होना अत्यंत अनिवार्य है। \n" +
            "  आज इसके अभाव में सारे विश्व में ईर्ष्या द्वेष, वैर विरोध,लूटपाट,छल कपट,घृणा,हत्या, अशान्ति आदि समस्याएं चहुं ओर स्पष्ट दिखाई दे रही हैं। मुख्य कारण है धर्म को ठीक से न जानना या फिर उल्टा पुल्टा जानना।\n" +
            "  राष्ट्रीय आर्य निर्मात्री सभा का उद्देश्य प्रत्येक मनुष्य को इन विकट स्थितियों से निकाल कर एक सभ्य, सुशिक्षित, चरित्रवान नागरिक बनाना व परस्पर सहयोगी, संगठित समाज का निर्माण करना है जिससे मनुष्य जन्म सफल करने को ज्ञान- विज्ञान और अनुकूल वातावरण सहज से प्राप्त हो सके।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य जितेन्द्र आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
          phoneNumber = "9416201731",
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य डॉ० महेशचन्द्र आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_mahesh.webp",
          email = "aryamaheshchander@gmail.com"
        ),
        post = "सत्र संयोजक",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "डॉ० महेश आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/dr_mahesh_arya.webp",
          email = "Mahesh.arya1975@gmail.com"
        ),
        post = "महासचिव",
        priority = 3
      ),
      OrganisationalMember(
        Member(
          name = "उपाचार्य जसबीर आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp"
        ),
        post = "सचिव",
        priority = 4
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य क्षत्रिय सभा",
    logo = "kshatriya_sabha",
    description = "राष्ट्रीय कर्तव्य के प्रति सजग, सबल और चरित्रवान युवा वर्ग का निर्माण करना।\n" +
        "युवाओं का स्वरक्षा, समाज रक्षा और राष्ट्र रक्षा के लिए निर्माण करते हुए उनको वैदिक परंपरा से अवगत करवाना।\n" +
        "युवा वर्ग को नशा आदि दुर्व्यसन से छुड़ाकर स्वास्थ्य एवं बुद्धिमान बनाना।\n" +
        "क्षत्रिय वर्ण का पुनरूद्धार कर पुनः स्थापित करना ।\n" +
        "समाज के प्रत्येक व्यक्ति को वैदिक वर्ण व्यवस्था से परिचित कराते हुए, उसके रक्षार्थ सन्नद्ध करना व समाज में एकता स्थापित करने के लिए उनका सर्वविध निर्माण करना।\n" +
        "सभी आर्यों, जिसमें आर्य संतति भी सम्मिलित है, को स्वास्थ्य रक्षा व आत्मरक्षा सिखाना है जिससे सभी आर्य व आर्यपुत्र स्वस्थ रहना सीख सकें और आपात परिस्थितियों में अपनी, अपने परिवार, आर्य संगठन व राष्ट्र रक्षा कर सकने के योग्य हो सकें।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "सौम्य आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9466944880",
          email = "Sonurana.rana@gmail.com"
        ),
        post = "सचिव",
        priority = 1
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य संरक्षिणी सभा",
    logo = "sanrakshini_sabha",
    description = "राष्ट्रीय आर्य संवर्धिनी सभा  एकमात्र वह संस्था है जो आर्य परिवारों के निर्माण, संरक्षण और उनके संवर्धन के लिए सदा प्रयासरत है। इस सभा की स्थापना आश्विन मास, शुक्लपक्ष, दशमी तिथि, विजयादशमी पर्व तदनुसार 24 अक्टूबर 2023 को आर्यसमाज शिवाजी कालोनी, रोहतक हरियाणा में सम्पन्न हुई थी। यह सभा आर्य महासंघ के अन्तर्गत  और उसके उद्देश्य अनुसार गतिविधियों को सम्पन्न करने के लिए बनाई गई है। वर्तमान में इस सभा के राष्ट्रीय अध्यक्ष आचार्य वर्चस्पति हिसार, राष्ट्रीय उपाध्यक्ष आर्य वेदप्रकाश रोहतक, राष्ट्रीय महासचिव आचार्य चरण सिंह भरतपुर , राष्ट्रीय कोषाध्यक्ष आर्य वेद गुरुग्राम, हरियाणा प्रान्त सचिव आर्य मनीराम, दिल्ली प्रान्त अध्यक्ष आचार्य राजेश और सचिव आर्य कप्तान, उत्तर प्रदेश प्रान्त अध्यक्ष आर्य भारत शास्त्री और सचिव आर्य रोबिन को मनोनीत किया गया है। इस सभा का राष्ट्रीय कार्यालय क्षात्र गुरुकुल, भाली आनन्दपुर, रोहतक में स्थित है। किसी भी देश, राष्ट्र और समाज की प्रथम इकाई परिवार ही होती है और परिवार श्रेष्ठ अर्थात् आर्य होना चाहिए, इसी उद्देश्य की पूर्ति के लिए यह सभा और इसके कार्यकर्ता अहर्निश कार्यरत हैं, धन्यवाद!",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आर्य प्रवेश 'प्रघोष'",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "7419002189",
          email = "aacharyaji@hotmail.com"
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य संजीव आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9045353309",
          email = "prachetas Arya@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य सुशील",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9410473224",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य संवर्धिनी सभा",
    logo = "sanvardhini_sabha",
    description = "राष्ट्रीय आर्य संवर्धिनी सभा  एकमात्र वह संस्था है जो आर्य परिवारों के निर्माण, संरक्षण और उनके संवर्धन के लिए सदा प्रयासरत है। इस सभा की स्थापना आश्विन मास, शुक्लपक्ष, दशमी तिथि, विजयादशमी पर्व तदनुसार 24 अक्टूबर 2023 को आर्यसमाज शिवाजी कालोनी, रोहतक हरियाणा में सम्पन्न हुई थी। यह सभा आर्य महासंघ के अन्तर्गत  और उसके उद्देश्य अनुसार गतिविधियों को सम्पन्न करने के लिए बनाई गई है। वर्तमान में इस सभा के राष्ट्रीय अध्यक्ष आचार्य वर्चस्पति हिसार, राष्ट्रीय उपाध्यक्ष आर्य वेदप्रकाश रोहतक, राष्ट्रीय महासचिव आचार्य चरण सिंह भरतपुर , राष्ट्रीय कोषाध्यक्ष आर्य वेद गुरुग्राम, हरियाणा प्रान्त सचिव आर्य मनीराम, दिल्ली प्रान्त अध्यक्ष आचार्य राजेश और सचिव आर्य कप्तान, उत्तर प्रदेश प्रान्त अध्यक्ष आर्य भारत शास्त्री और सचिव आर्य रोबिन को मनोनीत किया गया है। इस सभा का राष्ट्रीय कार्यालय क्षात्र गुरुकुल, भाली आनन्दपुर, रोहतक में स्थित है। किसी भी देश, राष्ट्र और समाज की प्रथम इकाई परिवार ही होती है और परिवार श्रेष्ठ अर्थात् आर्य होना चाहिए, इसी उद्देश्य की पूर्ति के लिए यह सभा और इसके कार्यकर्ता अहर्निश कार्यरत हैं, धन्यवाद!",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य वर्चस्पति",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9053347826",
          email = "acharyavarchaspati@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य वेदप्रकाश",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "8168491108",
          email = ""
        ),
        post = "उपाध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "उपाचार्य जसबीर आर्य ",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9871092222",
          email = ""
        ),
        post = "सचिव",
        priority = 3
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य दलितोद्धारिणी सभा",
    logo = "dalitoddharini_sabha",
    description = "हजारों वर्षों से शोषित और पीड़ित एक बहुत बड़ा समुदाय जिसको समाज से अलग-थलग कर दिया गया जो कभी आर्यों का एक भाग होता था और उसको भी वही सम्मान अधिकार प्राप्त था जो ब्राह्मण क्षत्रिय वैश्य को था आज उसको अलग कर दिया गया। उसी के उद्धार के लिए राष्ट्रीय दलितोद्धारिणी सभा बनाई गई है ताकि प्रत्येक मनुष्य समानता का अधिकार प्राप्त कर सके।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "सुखविंदर आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "8529616314",
          email = "sukhvinderarya@gmail.com"
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      ),
      OrganisationalMember(
        Member(
          name = "आर्य धर्मबीर शास्त्री",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9812428391",
          email = "dhrmbrarya@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य संदीप शास्त्री",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9812492102",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "महासचिव",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "आर्य गुरुकुल महाविद्यालय",
    logo = "ary_gurukul_mahavidyalaya",
    description = "सृष्टि के आदि से लेकर आजतक सत्यज्ञान हमें ऋषियों के द्वारा मिलता रहा है | ज्ञान के वाहक ऋषिगण होते हैं | कर्तव्य-अकर्तव्य, पुण्य-पाप, धर्म-अधर्म, गुण-अवगुण, लाभ-हानि, सत्य-असत्य, हितकर-अहितकर, आस्तिक-नास्तिक आदि तथा ईश्वर का परिज्ञान हम मनुष्यों को ऋषि मुनि ही बतलाते हैं | ऋषिगण अपूर्व मेधा सम्पन्न, ईश्वर के संविधान के महाविद्वान, निस्वार्थी और परम दयालु होते हैं | इनका प्रत्येक उपदेश और कार्य प्राणिमात्र के हित के लिये होता है | वर्तमान कालीन देश-प्रान्त आदि की सीमाओं में इनका ज्ञान और कार्य बंधा हुआ नहीं होता है, परन्तु इस विश्व में प्रत्येक मनुष्यमात्र के लिये इनका उपदेश और कार्य होता है, यथार्थ में ये ऋषि – मुनि ही देश काल की सीमाओं से परे जाकर मनुष्य मात्र के कल्याण और उन्नयन के लिये कर्म और उपदेश करते हैं, वास्तव में ये ऋषि – मुनि ही मनुष्य ही नहीं अपितु प्राणिमात्र के सच्चे हितैषी होते हैं, इनका उपदेश हिन्दु, मुस्लिम, ईसाई, पारसी, जैनी, बौद्ध आदि-आदि विश्व भर में प्रचलित समस्त मत – पन्थों एवं सम्प्रदायों के अनुयायियों के लिये भी एक जैसा होता है, ये ही सच्चे अर्थों में मानवीय होते हैं| ऋषियों का ज्ञान सत्य, तथ्य, तर्क और यथार्थ वैज्ञानिक सिद्धान्तों पर आधारित होता है| जिनके सिद्धान्तों को किसी भी काल में और किसी के भी द्वारा काटा नहीं जा सकता है, इनका सिद्धान्त ईश्वरीय सिद्धान्तों एवं उनके द्वारा प्रदत्त ज्ञान पर अवलम्बित है| सृष्टि के प्रारम्भ के ऋषियों से लेकर महाभारत कालीन ऋषियों यथा ऋषि व्यास , ऋषि जैमिनि, ऋषि पतन्जलि, ऋषि कणाद, ऋषि कपिल, ऋषि गौतम, ऋषि यास्क और पराधीनता के काल में ऋषि दयानन्द से हमें विश्व भर के मनुष्यमात्र के लिये करणीय और धारणीय ईश्वरीय ज्ञान मिला है|",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य संजीव आर्य ",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9045353309",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "अश्विनी आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9719375460",
          email = "saini.ashvani0@gmail.com"
        ),
        post = "सचिव",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "आर्या गुरुकुल महाविद्यालय",
    logo = "arya_gurukul_mahavidyalaya",
    description = "सृष्टि के आदि से लेकर आजतक सत्यज्ञान हमें ऋषियों के द्वारा मिलता रहा है | ज्ञान के वाहक ऋषिगण होते हैं | कर्तव्य-अकर्तव्य, पुण्य-पाप, धर्म-अधर्म, गुण-अवगुण, लाभ-हानि, सत्य-असत्य, हितकर-अहितकर, आस्तिक-नास्तिक आदि तथा ईश्वर का परिज्ञान हम मनुष्यों को ऋषि मुनि ही बतलाते हैं | ऋषिगण अपूर्व मेधा सम्पन्न, ईश्वर के संविधान के महाविद्वान, निस्वार्थी और परम दयालु होते हैं | इनका प्रत्येक उपदेश और कार्य प्राणिमात्र के हित के लिये होता है | वर्तमान कालीन देश-प्रान्त आदि की सीमाओं में इनका ज्ञान और कार्य बंधा हुआ नहीं होता है, परन्तु इस विश्व में प्रत्येक मनुष्यमात्र के लिये इनका उपदेश और कार्य होता है, यथार्थ में ये ऋषि – मुनि ही देश काल की सीमाओं से परे जाकर मनुष्य मात्र के कल्याण और उन्नयन के लिये कर्म और उपदेश करते हैं, वास्तव में ये ऋषि – मुनि ही मनुष्य ही नहीं अपितु प्राणिमात्र के सच्चे हितैषी होते हैं, इनका उपदेश हिन्दु, मुस्लिम, ईसाई, पारसी, जैनी, बौद्ध आदि-आदि विश्व भर में प्रचलित समस्त मत – पन्थों एवं सम्प्रदायों के अनुयायियों के लिये भी एक जैसा होता है, ये ही सच्चे अर्थों में मानवीय होते हैं| ऋषियों का ज्ञान सत्य, तथ्य, तर्क और यथार्थ वैज्ञानिक सिद्धान्तों पर आधारित होता है| जिनके सिद्धान्तों को किसी भी काल में और किसी के भी द्वारा काटा नहीं जा सकता है, इनका सिद्धान्त ईश्वरीय सिद्धान्तों एवं उनके द्वारा प्रदत्त ज्ञान पर अवलम्बित है| सृष्टि के प्रारम्भ के ऋषियों से लेकर महाभारत कालीन ऋषियों यथा ऋषि व्यास , ऋषि जैमिनि, ऋषि पतन्जलि, ऋषि कणाद, ऋषि कपिल, ऋषि गौतम, ऋषि यास्क और पराधीनता के काल में ऋषि दयानन्द से हमें विश्व भर के मनुष्यमात्र के लिये करणीय और धारणीय ईश्वरीय ज्ञान मिला है|",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्या इन्द्रा",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9868912128",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आचार्या डॉ० सुशीला",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9719375460",
          email = "saini.ashvani0@gmail.com"
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आर्या रेनु",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9999999999",
          email = "" +
              ""
        ),
        post = "कोषाध्यक्ष",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "आर्या परिषद्",
    logo = "arya_parishad",
    description = "सृष्टि के आदि से लेकर आजतक सत्यज्ञान हमें ऋषियों के द्वारा मिलता रहा है | ज्ञान के वाहक ऋषिगण होते हैं | कर्तव्य-अकर्तव्य, पुण्य-पाप, धर्म-अधर्म, गुण-अवगुण, लाभ-हानि, सत्य-असत्य, हितकर-अहितकर, आस्तिक-नास्तिक आदि तथा ईश्वर का परिज्ञान हम मनुष्यों को ऋषि मुनि ही बतलाते हैं | ऋषिगण अपूर्व मेधा सम्पन्न, ईश्वर के संविधान के महाविद्वान, निस्वार्थी और परम दयालु होते हैं | इनका प्रत्येक उपदेश और कार्य प्राणिमात्र के हित के लिये होता है | वर्तमान कालीन देश-प्रान्त आदि की सीमाओं में इनका ज्ञान और कार्य बंधा हुआ नहीं होता है, परन्तु इस विश्व में प्रत्येक मनुष्यमात्र के लिये इनका उपदेश और कार्य होता है, यथार्थ में ये ऋषि – मुनि ही देश काल की सीमाओं से परे जाकर मनुष्य मात्र के कल्याण और उन्नयन के लिये कर्म और उपदेश करते हैं, वास्तव में ये ऋषि – मुनि ही मनुष्य ही नहीं अपितु प्राणिमात्र के सच्चे हितैषी होते हैं, इनका उपदेश हिन्दु, मुस्लिम, ईसाई, पारसी, जैनी, बौद्ध आदि-आदि विश्व भर में प्रचलित समस्त मत – पन्थों एवं सम्प्रदायों के अनुयायियों के लिये भी एक जैसा होता है, ये ही सच्चे अर्थों में मानवीय होते हैं| ऋषियों का ज्ञान सत्य, तथ्य, तर्क और यथार्थ वैज्ञानिक सिद्धान्तों पर आधारित होता है| जिनके सिद्धान्तों को किसी भी काल में और किसी के भी द्वारा काटा नहीं जा सकता है, इनका सिद्धान्त ईश्वरीय सिद्धान्तों एवं उनके द्वारा प्रदत्त ज्ञान पर अवलम्बित है| सृष्टि के प्रारम्भ के ऋषियों से लेकर महाभारत कालीन ऋषियों यथा ऋषि व्यास , ऋषि जैमिनि, ऋषि पतन्जलि, ऋषि कणाद, ऋषि कपिल, ऋषि गौतम, ऋषि यास्क और पराधीनता के काल में ऋषि दयानन्द से हमें विश्व भर के मनुष्यमात्र के लिये करणीय और धारणीय ईश्वरीय ज्ञान मिला है|",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्या डॉ० सुशीला",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9719375460",
          email = "saini.ashvani0@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
    )
  ),
  Sabha(
    name = "वानप्रस्थ आयोग",
    logo = "vanprasth_ayog",
    description = "ब्रह्मचर्याश्रमं समाप्य गृही भवेद् गृही भूत्वा वनी भवेद्\n" +
        "मनुष्यों को उचित है की ब्रह्मचर्य आश्रम को समाप्त करके गृहस्थ होकर वानप्रस्थ होवें। यह अनुक्रम से आश्रम का विधान है।\n" +
        "जब गृहस्थ शिर के श्वेत केश और त्वचा ढीली हो जय और लड़के को लड़का हो गया हो तब घर का राग छोड़, स्वाध्याय अर्थात पढ़ने पढ़ाने में नित्ययुक्त, जीवात्मा, सब का मित्र, इन्द्रियों का दमनशील, सब पर दयालु और शरीरसुख के लिए अति प्रयत्न न करें। अपने आश्रित और स्वकीय पदार्थो में ममता न करें।\n" +
        "वानप्रस्थ को उचित है की अग्नि में होम कर दीक्षित व्रत, सत्याचरण, नाना प्रकार की तपश्चर्या, सत्संग, योगाभ्यास, सुविचार से ज्ञान और पवित्रता प्राप्त करें।\n" +
        "इसी उद्देश्य की पूर्ती हेतु आर्य महासंघ के निर्देशन में वानप्रस्थ आयोग का गठन हुआ है।\n",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "पंडित लोकनाथजी आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9868912128",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "श्री वेदप्रकाश आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9719375460",
          email = "saini.ashvani0@gmail.com"
        ),
        post = "उपाध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "श्री शिवनारायणजी आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9999999999",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य छात्र सभा",
    logo = "chatra_sabha",
    description = "छात्र सभा का उद्देश्य आर्य संतति अर्थात विश्व भर के आर्य परिवार के बालक बालिकाओं को सही समय पर आर्यत्व में स्थापित करना है जिससे आर्य परिवार की भावी पीढ़ी उच्च आधुनिक शिक्षा के साथ- साथ अपनी वैदिक संस्कृति, श्रेष्ठ परंपराओं, मानव मूल्यों से युक्त स्वस्थ व सबल बने" +
                  "छात्र सभा का मुख्य उद्देश्य:\n" +
        "1. आधुनिक शिक्षा: आर्य परिवार के बालक बालिका न्यूनतम स्नातक तक की शिक्षा अवश्य लेना सुनिश्चित करना व उच्च से उच्च शिक्षा ग्रहण करने के लिए प्रोत्साहित करना\n" +
        "2. स्वास्थ्य रक्षा: आर्य परिवार का बालक बालिकाएं उत्तम स्वास्थ्य के लिए उचित खान-पान, व्यायाम, दिनचर्या ऋतुचार्य आदि का महत्व ज्ञान व व्यवहारिक अभ्यास उपलब्ध कराना\n" +
        "3. आत्मरक्षा: आपात स्थितियां ,विपरीत परिस्थितियों, आवश्यकता पड़ने पर अपनी अपने परिवार आर्य संगठन व राष्ट्र रक्षा कर सकने के योग्य बनाना व परस्पर सहयोग करना\n" +
        "4. नैतिकता की रक्षा: प्रत्येक आर्य परिवार के बालक बालिकाओं की आज के परिवेश में ग्लोबलाइजेशन आधुनिकता आदि के नाम पर नैतिक पतन को रोकना व उनके आर्यत्व को बचाए रखने, बनाए रखने में समर्थ करना\n" +
        "5. आर्य छात्रों को व्यक्तिगत तथा करियर संबंधित मार्गदर्शन उपलब्ध कराना",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "अनिल आर्य खेदड़",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9034763824",
          email = "hpgclanil@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "बिजेंद्र सिंह",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9416037102",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "राष्ट्रीय आर्य संचार परिषद",
    logo = "sanchar_parishad",
    description = "आर्य महासंघ का संचार प्रकल्प",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "अनिल आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9416037102",
          email = ""
        ),
        post = "सचिव",
        priority = 2
      )
    )
  ),
  Sabha(
    name = "आर्य महासंघ",
    logo = "mahasangh_logo_without_background",
    description = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।\n" +
        "\n" +
        "आचार्य हनुमत प्रसाद\n" +
        "अध्यक्ष, आर्य महासंघ",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य हनुमत् प्रसाद",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_hanumat_prasad.webp",
          phoneNumber = "9868792232",
          email = ""
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य सतीश",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_satish.webp",
          phoneNumber = "9350945482",
          email = ""
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आर्य जसबीर सिंह",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9868792232",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      )
    )
  )
//  Sabha(name = "केंद्रिय वित्तीय प्रबंधन परिषद", logo = "", description = ""),
)