package org.aryamahasangh

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val activityNamesData = listOf(
  "आर्य प्रशिक्षण सत्र",
  "आर्या प्रशिक्षण सत्र",
  "भव्य शिलान्यास समारोह",
  "नियमित संध्या अनुष्ठान अभियान",
  "सिद्धालंकार कक्षा",
  "सिद्धालंकार कक्षा -(आर्यााओं के लिए)",
  "आत्मरक्षा प्रशिक्षण - आर्य बालिकाओं के लिए",
  "छात्र आत्मरक्षा प्रशिक्षण",
  "श्रावणी उपाकर्म",
  "आर्ष गुरुकुल - उद्गाठन एवं नवसंवत्सर यज्ञ",
  "विजयादशी पर्व",
  "सनातन धर्म संस्कार शिविर",
  "सांगठनिक बैठक"
)

val listOfCities = "फरीदाबाद, गुड़गांव, मेवात, रोहतक, सोनीपत, रेवाड़ी, झज्जर, पानीपत, पलवल, महेंद्रगढ़, भिवानी,जींद, मेरठ, गाजियाबाद, गौतम बुद्ध नगर (नोएडा), ग्रेटर नोएडा, बुलंदशहर, बागपत, हापुड़, मुजफ्फरनगर"

val activities = generateRandomActivities(20)



@OptIn(ExperimentalUuidApi::class)
fun generateRandomActivities(count: Int): List<OrganisationalActivity> {
  val activityNames = activityNamesData
  val descriptions = listOf(
    "महान सनातन परम्पराओं को जानने का प्रकल्प",
    "आप सभी को सूचित करते हुए बड़ी प्रसन्नता हो रही है कि ईश्वरानुकम्पा से आर्य पदाधिकारी परिषद के प्रथम अधिवेशन का भव्य आयोजन 03 सितम्बर 2023 रविवार को क्षात्र गुरुकुल भाली आनन्दपुर, रोहतक हरियाणा में किया जा रहा है।",
    "आर्य महासंघ के तत्वावधान में राष्ट्रीय आर्य निर्मात्री सभा द्वारा दो दिवसीय आर्य प्रशिक्षण शिविर का आयोजन हरियाणा प्रान्त के नूह जनपद के पुन्हाना शहर में किया गया, जिसमे आदरणीय आचार्या मोनिका जी सभी माताओं-बहनों कों अपने ऋषि मुनियों की श्रेष्ठ विद्या अर्थात वेद विद्या से अवगत करवाती हुई",
    "ईश के ज्ञान से लोक में जांच के आर्य कार्य आगे बढ़ाते रहें। नित्य है ना मिटे ना हटे ले चले प्रार्थना प्रेम से भाव लाते रहें।",
    "सिद्धांतालंकारा कक्षा के दूसरे समैस्टर की कक्षा का समापन हुआ।"
  )
  val mediaUrls = listOf(
    "https://images.pexels.com/photos/209831/pexels-photo-209831.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
    "https://images.pexels.com/photos/1624496/pexels-photo-1624496.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
    "https://images.pexels.com/photos/1402787/pexels-photo-1402787.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150"
  )
  val posts = listOf("Coordinator", "Volunteer", "Manager", "Organizer")
  val cities = listOfCities.split(", ")

  return List(count) {
    val now = Clock.System.now()
    val systemTZ = TimeZone.currentSystemDefault()
    val start = now.plus(Random.nextLong(1, 30), DateTimeUnit.DAY, systemTZ)
    val end = start.plus(Random.nextLong(2, 288), DateTimeUnit.DAY, systemTZ)
      .plus(Random.nextLong(1, 10), DateTimeUnit.HOUR, systemTZ)

    OrganisationalActivity(
      id = Uuid.random().toString(),
      name = activityNames.random(),
      description = descriptions.random(),
      activityType = ActivityType.entries.toTypedArray().random(),
      place = cities.random(),
      associatedOrganisation = listOf(
        "राष्ट्रीय आर्य निर्मात्री सभा",
        "राष्ट्रीय आर्य क्षत्रिय सभा",
        "राष्ट्रीय आर्य संरक्षिणी सभा",
        "राष्ट्रीय आर्य संवर्धिनी सभा",
        "राष्ट्रीय आर्य दलितोद्धारिणी सभा",
        "आर्य गुरुकुल महाविद्यालय",
        "आर्या गुरुकुल महाविद्यालय",
        "आर्या परिषद्",
        "वानप्रस्थ आयोग",
        "राष्ट्रीय आर्य छात्र सभा",
        "राष्ट्रीय आर्य संचार परिषद",
        "आर्य महासंघ"
      ).shuffled().takeLast(Random.nextInt(1, 3)),
      startDateTime = start.toLocalDateTime(systemTZ),
      endDateTime = end.toLocalDateTime(systemTZ),
      mediaFiles = List(Random.nextInt(1, 3)) { mediaUrls.random() },
      contactPeople = listOf(
        OrganisationalMember(
          member = Member(
            name = "आचार्य जितेन्द्र आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
            phoneNumber = "9416201731",
          ),
          post = "अध्यक्ष",
          priority = 1
        ),
        OrganisationalMember(
          member = Member(
            name = "आचार्य डॉ० महेशचन्द्र आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_mahesh.webp",
            email = "aryamaheshchander@gmail.com",
            phoneNumber = "9416201731",
          ),
          post = "सत्र संयोजक",
          priority = 2
        ),
        OrganisationalMember(
          member = Member(
            name = "डॉ० महेश आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/dr_mahesh_arya.webp",
            email = "Mahesh.arya1975@gmail.com",
            phoneNumber = "9416201731",
          ),
          post = "महासचिव",
          priority = 3
        ),
        OrganisationalMember(
          member = Member(
            name = "उपाचार्य जसबीर आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
            phoneNumber = "9416201731",
          ),
          post = "सचिव",
          priority = 4
        ),
        OrganisationalMember(
          member = Member(
            name = "सौम्य आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
            phoneNumber = "9466944880",
            email = "Sonurana.rana@gmail.com"
          ),
          post = "सचिव",
          priority = 1
        ),
        OrganisationalMember(
          member = Member(
            name = "आर्य प्रवेश 'प्रघोष'",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_pravesh_ji.webp",
            phoneNumber = "7419002189",
            email = "aacharyaji@hotmail.com"
          ),
          post = "महासचिव",
          priority = 2
        ),
        OrganisationalMember(
          member = Member(
            name = "आचार्य संजीव आर्य",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
            phoneNumber = "9045353309",
            email = "prachetas Arya@gmail.com"
          ),
          post = "अध्यक्ष",
          priority = 1
        ),
        OrganisationalMember(
          member = Member(
            name = "आर्य सुशील",
            profileImage = "",
            phoneNumber = "9410473224",
            email = ""
          ),
          post = "कोषाध्यक्ष",
          priority = 3
        ),
        OrganisationalMember(
          member = Member(
            name = "आचार्य वर्चस्पति",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_varchaspati.webp",
            phoneNumber = "9053347826",
            email = "acharyavarchaspati@gmail.com"
          ),
          post = "अध्यक्ष",
          priority = 1
        ),
        OrganisationalMember(
          member = Member(
            name = "आर्य वेदप्रकाश",
            profileImage = "",
            phoneNumber = "8168491108",
            email = ""
          ),
          post = "उपाध्यक्ष",
          priority = 2
        ),
        OrganisationalMember(
          member = Member(
            name = "उपाचार्य जसबीर आर्य ",
            profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
            phoneNumber = "9871092222",
            email = ""
          ),
          post = "सचिव",
          priority = 3
        )
      ).shuffled().takeLast(Random.nextInt(1,4)),
      additionalInstructions = listOf(
        "सत्यार्थ प्रकाश का पढ़ना और पढ़ाना।",
        "सन्ध्या करना और करवाना।",
        "शान्तिपाठ + जयघोष अभिवादन + प्रसाद वितरण।",
        "सभी आर्यगण आचार्य जी को सुनने के लिए लिंक पर क्लिक करके समय से जुड़ें व आर्य महासंघ के फ़ेसबुक पेज को अभी फॉलो करें।",
        "सभी आर्यसमाज पदाधिकारी अवश्य पहुंचें और संगठित स्वरूप को प्रकाशित करें !!"
      ).shuffled().take(Random.nextInt(2,4)).joinToString(separator = "\n")
    )
  }
}

@OptIn(ExperimentalUuidApi::class)
val listOfOrganisations = mutableListOf(
  Organisation(
    id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य निर्मात्री सभा",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//nirmatri_sabha.webp",
    description = "प्रत्येक बुद्धिमान व्यक्ति यह समझ सकता है कि मानवीय जीवन अति दुर्लभ है। " +
        "लेकिन विडम्बना है कि इसके प्रयोजन को ढूंढने पर कोई विरला ही विचार करता है। " +
        "जीवन की सफलता के लिए आवश्यक है कि हम जानें कि इससे हम क्या प्राप्त कर सकते हैं और कैसे? " +
        "वास्तव में इन प्रश्नों के उत्तर हेतु धर्म के सत्य स्वरूप का ज्ञान होना अत्यंत अनिवार्य है। \n" +
        "  आज इसके अभाव में सारे विश्व में ईर्ष्या द्वेष, वैर विरोध,लूटपाट,छल कपट,घृणा,हत्या, अशान्ति आदि समस्याएं चहुं ओर स्पष्ट दिखाई दे रही हैं। मुख्य कारण है धर्म को ठीक से न जानना या फिर उल्टा पुल्टा जानना।\n" +
        "  राष्ट्रीय आर्य निर्मात्री सभा का उद्देश्य प्रत्येक मनुष्य को इन विकट स्थितियों से निकाल कर एक सभ्य, सुशिक्षित, चरित्रवान नागरिक बनाना व परस्पर सहयोगी, संगठित समाज का निर्माण करना है जिससे मनुष्य जन्म सफल करने को ज्ञान- विज्ञान और अनुकूल वातावरण सहज से प्राप्त हो सके।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य जितेन्द्र आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
          phoneNumber = "9416201731",
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य डॉ० महेशचन्द्र आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_mahesh.webp",
          phoneNumber = "9813377510",
          email = "aryamaheshchander@gmail.com"
        ),
        post = "सत्र संयोजक",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "डॉ० महेश आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/dr_mahesh_arya.webp",
          phoneNumber = "9810485231",
          email = "Mahesh.arya1975@gmail.com"
        ),
        post = "महासचिव",
        priority = 3
      ),
      OrganisationalMember(
        Member(
          name = "उपाचार्य जसबीर आर्य",
          phoneNumber = "9871092222",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp"
        ),
        post = "सचिव",
        priority = 4
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य क्षत्रिय सभा",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//kshatriya_sabha.webp",
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
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9466944880",
          email = "Sonurana.rana@gmail.com"
        ),
        post = "सचिव",
        priority = 1
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य संरक्षिणी सभा",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//sanrakshini_sabha.webp",
    description = "(आर्य महासंघ का एक अति महत्वपूर्ण घटक)\n" +
        " आर्य निर्माण की सुदीर्घ और अत्यंत महत्वपूर्ण प्रक्रिया में राष्ट्रीय आर्य संरक्षिणी सभा और उसका कार्य बहुत महत्व रखता है । 'राष्ट्रीय आर्य निर्मात्री सभा' के द्वारा आर्य निर्माण किए जाने के पश्चात् आर्य सिद्धांतों से सहमत हुए सद्योपवीति आर्यों का संरक्षण और आर्यों /आर्याओं को स्थानीय स्तर पर एक सूत्र में बाँधकर संगठित करने के लिए आर्य समाजों की स्थापना और उनका संचालन /संरक्षण इस सभा का प्रमुख कार्य है । साथ ही सभा का उद्देश्य है कि संसार भर में कहीं भी कोई आर्य हो उसका संरक्षण किया जा सके, उसके लिए सभा सर्वाधिक उपाय करती रहती है । आर्य निर्माण से राष्ट्र निर्माण की शुभेच्छा और संकल्प को यदि सिद्ध करना है तो हमें 'आर्यसमाज' नाम की संस्था से वास्तविक आर्यसमाज बनना होगा उसके लिए एक आर्य का विवाह आर्या और आर्या का विवाह आर्य के साथ गुणकर्म -स्वभावानुसार करके प्रत्येक आर्य की संतान का आर्य/आर्या बनना अत्यंत आवश्यक है और राष्ट्रीय आर्य संरक्षिणी सभा आर्य समाजों के माध्यम से इस आधारभूत कार्य को करने के लिए कृत संकल्पित है।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आर्य प्रवेश 'प्रघोष'",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_pravesh_ji.webp",
          phoneNumber = "7419002189",
          email = "aacharyaji@hotmail.com"
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य संजीव आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
          phoneNumber = "9045353309",
          email = "prachetas Arya@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य सुशील",
          profileImage = "",
          phoneNumber = "9410473224",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य संवर्धिनी सभा",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//sanvardhini_sabha.webp",
    description = "राष्ट्रीय आर्य संवर्धिनी सभा  एकमात्र वह संस्था है जो आर्य परिवारों के निर्माण, संरक्षण और उनके संवर्धन के लिए सदा प्रयासरत है। इस सभा की स्थापना आश्विन मास, शुक्लपक्ष, दशमी तिथि, विजयादशमी पर्व तदनुसार 24 अक्टूबर 2023 को आर्यसमाज शिवाजी कालोनी, रोहतक हरियाणा में सम्पन्न हुई थी। यह सभा आर्य महासंघ के अन्तर्गत  और उसके उद्देश्य अनुसार गतिविधियों को सम्पन्न करने के लिए बनाई गई है। वर्तमान में इस सभा के राष्ट्रीय अध्यक्ष आचार्य वर्चस्पति हिसार, राष्ट्रीय उपाध्यक्ष आर्य वेदप्रकाश रोहतक, राष्ट्रीय महासचिव आचार्य चरण सिंह भरतपुर , राष्ट्रीय कोषाध्यक्ष आर्य वेद गुरुग्राम, हरियाणा प्रान्त सचिव आर्य मनीराम, दिल्ली प्रान्त अध्यक्ष आचार्य राजेश और सचिव आर्य कप्तान, उत्तर प्रदेश प्रान्त अध्यक्ष आर्य भारत शास्त्री और सचिव आर्य रोबिन को मनोनीत किया गया है। इस सभा का राष्ट्रीय कार्यालय क्षात्र गुरुकुल, भाली आनन्दपुर, रोहतक में स्थित है। किसी भी देश, राष्ट्र और समाज की प्रथम इकाई परिवार ही होती है और परिवार श्रेष्ठ अर्थात् आर्य होना चाहिए, इसी उद्देश्य की पूर्ति के लिए यह सभा और इसके कार्यकर्ता अहर्निश कार्यरत हैं, धन्यवाद!",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य वर्चस्पति",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_varchaspati.webp",
          phoneNumber = "9053347826",
          email = "acharyavarchaspati@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य वेदप्रकाश",
          profileImage = "",
          phoneNumber = "8168491108",
          email = ""
        ),
        post = "उपाध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "उपाचार्य जसबीर आर्य ",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9871092222",
          email = ""
        ),
        post = "सचिव",
        priority = 3
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य दलितोद्धारिणी सभा",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//dalitoddharini_sabha.webp",
    description = "हजारों वर्षों से शोषित और पीड़ित एक बहुत बड़ा समुदाय जिसको समाज से अलग-थलग कर दिया गया जो कभी आर्यों का एक भाग होता था और उसको भी वही सम्मान अधिकार प्राप्त था जो ब्राह्मण क्षत्रिय वैश्य को था आज उसको अलग कर दिया गया। उसी के उद्धार के लिए राष्ट्रीय दलितोद्धारिणी सभा बनाई गई है ताकि प्रत्येक मनुष्य समानता का अधिकार प्राप्त कर सके।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "सुखविंदर आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp",
          phoneNumber = "8529616314",
          email = "sukhvinderarya@gmail.com"
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      ),
      OrganisationalMember(
        Member(
          name = "आर्य धर्मबीर शास्त्री",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9812428391",
          email = "dhrmbrarya@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आर्य संदीप शास्त्री",
          profileImage = "",
          phoneNumber = "9812492102",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "महासचिव",
        priority = 2
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "आर्य गुरुकुल महाविद्यालय",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//ary_gurukul_mahavidyalaya.webp",
    description = "आर्य गुरुकुल महाविद्यालय (आर्य महासंघ द्वारा संचालित आर्य विद्या का उपक्रम)\n" +
        " आर्य गुरुकुल महाविद्यालय हजारों वर्षों से बाधित आर्य विद्या के प्रवाह को ऋषि दयानंद और पूरी ऋषि परंपरा के अनुसार स्थापित करने के लिए प्रयत्नरतऔर प्रतिबद्ध है उसके लिए आर्य गुरुकुल महाविद्यालय विभिन्न प्रकार के विशिष्ट प्रशिक्षण एवं कक्षाएं संचालित करता है वह पाठ्यक्रम निम्न है \n" +
        "(१) आर्य संरक्षक प्रशिक्षण \n" +
        "(२) आर्य समाज संचालक प्रशिक्षण \n" +
        "(३) आर्य समाज संरक्षक प्रशिक्षण \n" +
        "(४) आर्य समाज संरक्षक प्रशिक्षण \n" +
        "(५) संध्या प्रशिक्षण \n" +
        "(६) अग्निहोत्र प्रशिक्षण \n" +
        "(७) आर्य जीवन शाला \n" +
        "(८) योग शिविर (अष्टांग योग प्रशिक्षण ) \n" +
        "(९) वानप्रस्थ प्रशिक्षण (१०) पंचवर्षीय क्षात्र प्रशिक्षण (छात्र गुरुकुल) \n" +
        "(११) छात्र शिक्षक प्रशिक्षण \n" +
        "(१२) सिद्धांतालंकार कक्षा \n" +
        "(१३) आर्य पुरोहित कक्षा \n" +
        "(१४) आर्य प्रवक्ता कक्षा \n" +
        "(१५) आचार्य पात्रता (पूरक प्रशिक्षण) \n" +
        "(१६) आचार्य कक्षा\n" +
        " इन सभी कक्षाओं और भविष्य में भी संचालित होने वाले पाठ्यक्रमों के लिए सभी आर्यों से सक्रिय सहयोग की आकांक्षा करते हैं",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य संजीव आर्य ",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
          phoneNumber = "9045353309",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "अश्विनी आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_ashvani.webp",
          phoneNumber = "9719375460",
          email = "saini.ashvani0@gmail.com"
        ),
        post = "सचिव",
        priority = 2
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "आर्या गुरुकुल महाविद्यालय",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//arya_gurukul_mahavidyalaya.webp",
    description = "आर्य महासंघ के तत्वाधान में आर्या निर्माण ग्राम-ग्राम, नगर-नगर चल रहा है। अब तक ५० हजार से ऊपर महिलाओं का आर्याकरण हो चूका है। इस अभियान की निरंतरता के लिए वैदिक विदुषियों की आवश्यकता है। जिसके लिए आर्या गुरुकुल का निर्माण किया गया है। ६ अक्टूबर २०१९ को इसका उद्घाटन हुआ तब से यहाँ आर्या निर्माण, आचार्या निर्माण की कक्षाएं निरंतर चल रही है।  आर्य परिवारों की बालिकाओं के बौद्धिक व शारीरिक उन्नति के लिए क्षात्र प्रशिक्षण शिविरों का आयोजन गुरुकुल में होता है। ५ अप्रैल २०२४ से आर्या गुरुकुल महाविद्यालय के अंतर्गत आर्ष कन्या गुरुकुल का प्रारम्भ हुआ है। \n" +
        "अष्टाध्यायी व्याकरण आदि विषयों के माध्यम से संस्कृत को प्राथमिकता देकर छात्राओं को विदुषी बनाना गुरुकुल का उद्देश्य है। \n" +
        "वर्तमान में आचार्या डॉ. सुशीला गुरुकुल की आचार्या है।  \n" +
        "यह गुरुकुल आर्य महासंघ के निर्देशानुसार संचालित हो रहा है।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्या इन्द्रा",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_indra.webp",
          phoneNumber = "9868912128",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आचार्या डॉ० सुशीला",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_suman.webp",
          phoneNumber = "9355690824",
          email = ""
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आर्या रेनु",
          profileImage = "",
          phoneNumber = "9999999999",
          email = "" +
              ""
        ),
        post = "कोषाध्यक्ष",
        priority = 2
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "आर्या परिषद्",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//arya_parishad.webp",
    description = "आर्या परिषद् का गठन आर्याओं के हित, आर्याओं के निर्माण व् आर्याओं के संरक्षण के लिए किया गया है। \n" +
        "\n" +
        "इसके अंतर्गत केंद्रीय आर्या परिषद्, प्रांतीय आर्या परिषद् व् जनपद आर्या परिषद् का गठन किया गया है।  \n" +
        "आर्या परिषद्  की केंद्रीय अध्यक्षा आचार्या डॉ. सुशीला जी, केंद्रीय सचिव आचार्या डॉ मोनिका जी व केंद्रीय कोषाध्यक्ष आचार्या डॉ सुमन जी है।  \n" +
        "प्रान्त और जनपद स्तर पर आर्या परिषद् की १. अध्यक्षा २. निर्मात्री सचिव ३. संरक्षिणी सचिव ४. संवर्धनी सचिव नियुक्त की गई है।  \n" +
        "\n" +
        "इन पदाधिकारियों के साथ इनकी सहयोगी संस्थाएँ भी टीम में है। ये सभी पदाधिकारी व इनकी टीम की सदस्याएं महिलाओं के निर्माण व संरक्षण का कार्य करती है। ",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्या डॉ० सुशीला",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_indra.webp",
          phoneNumber = "9355690824",
          email = ""
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "वानप्रस्थ आयोग",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//vanprasth_ayog.webp",
    description = "\"ब्रह्मचर्याश्रमं समाप्य गृही भवेद् गृही भूत्वा वनी भवेद्\"\n" +
        "मनुष्यों को उचित है की ब्रह्मचर्य आश्रम को समाप्त करके गृहस्थ होकर वानप्रस्थ होवें। यह अनुक्रम से आश्रम का विधान है।\n" +
        "जब गृहस्थ शिर के श्वेत केश और त्वचा ढीली हो जय और लड़के को लड़का हो गया हो तब घर का राग छोड़, स्वाध्याय अर्थात पढ़ने पढ़ाने में नित्ययुक्त, जीवात्मा, सब का मित्र, इन्द्रियों का दमनशील, सब पर दयालु और शरीरसुख के लिए अति प्रयत्न न करें। अपने आश्रित और स्वकीय पदार्थो में ममता न करें।\n" +
        "वानप्रस्थ को उचित है की अग्नि में होम कर दीक्षित व्रत, सत्याचरण, नाना प्रकार की तपश्चर्या, सत्संग, योगाभ्यास, सुविचार से ज्ञान और पवित्रता प्राप्त करें।\n" +
        "इसी उद्देश्य की पूर्ती हेतु आर्य महासंघ के निर्देशन में वानप्रस्थ आयोग का गठन हुआ है।\n",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "पंडित लोकनाथजी आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_loknath.webp",
          phoneNumber = "7015563934",
          email = "deepphotostatebk@gmail.com"
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "श्री वेदप्रकाश आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_vedprakash.webp",
          phoneNumber = "8168491108",
          email = ""
        ),
        post = "उपाध्यक्ष",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "श्री शिवनारायणजी आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_shivnarayan.webp",
          phoneNumber = "9466140987",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      )
    )
  ),
//  Organisation(id = Uuid.random().toString(),
//    name = "राष्ट्रीय आर्य छात्र सभा",
//    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//chatra_sabha.webp",
//    description = "छात्र सभा का उद्देश्य आर्य संतति अर्थात विश्व भर के आर्य परिवार के बालक बालिकाओं को सही समय पर आर्यत्व में स्थापित करना है जिससे आर्य परिवार की भावी पीढ़ी उच्च आधुनिक शिक्षा के साथ- साथ अपनी वैदिक संस्कृति, श्रेष्ठ परंपराओं, मानव मूल्यों से युक्त स्वस्थ व सबल बने" +
//        "छात्र सभा का मुख्य उद्देश्य:\n" +
//        "1. आधुनिक शिक्षा: आर्य परिवार के बालक बालिका न्यूनतम स्नातक तक की शिक्षा अवश्य लेना सुनिश्चित करना व उच्च से उच्च शिक्षा ग्रहण करने के लिए प्रोत्साहित करना\n" +
//        "2. स्वास्थ्य रक्षा: आर्य परिवार का बालक बालिकाएं उत्तम स्वास्थ्य के लिए उचित खान-पान, व्यायाम, दिनचर्या ऋतुचार्य आदि का महत्व ज्ञान व व्यवहारिक अभ्यास उपलब्ध कराना\n" +
//        "3. आत्मरक्षा: आपात स्थितियां ,विपरीत परिस्थितियों, आवश्यकता पड़ने पर अपनी अपने परिवार आर्य संगठन व राष्ट्र रक्षा कर सकने के योग्य बनाना व परस्पर सहयोग करना\n" +
//        "4. नैतिकता की रक्षा: प्रत्येक आर्य परिवार के बालक बालिकाओं की आज के परिवेश में ग्लोबलाइजेशन आधुनिकता आदि के नाम पर नैतिक पतन को रोकना व उनके आर्यत्व को बचाए रखने, बनाए रखने में समर्थ करना\n" +
//        "5. आर्य छात्रों को व्यक्तिगत तथा करियर संबंधित मार्गदर्शन उपलब्ध कराना",
//    keyPeople = listOf(
//      OrganisationalMember(
//        Member(
//          name = "अनिल आर्य खेदड़",
//          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
//          phoneNumber = "9034763824",
//          email = "hpgclanil@gmail.com"
//        ),
//        post = "अध्यक्ष",
//        priority = 1
//      ),
//      OrganisationalMember(
//        Member(
//          name = "बिजेंद्र सिंह",
//          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
//          phoneNumber = "9416037102",
//          email = ""
//        ),
//        post = "कोषाध्यक्ष",
//        priority = 2
//      )
//    )
//  ),
  Organisation(id = Uuid.random().toString(),
    name = "राष्ट्रीय आर्य संचार परिषद",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//sanchar_parishad.webp",
    description = "आर्य महासंघ का संचार प्रकल्प",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "अनिल आर्य",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp",
          phoneNumber = "9416037102",
          email = ""
        ),
        post = "सचिव",
        priority = 2
      )
    )
  ),
  Organisation(id = Uuid.random().toString(),
    name = "आर्य महासंघ",
    logo = "mahasangh_logo_without_background",
    description = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।",
    keyPeople = listOf(
      OrganisationalMember(
        Member(
          name = "आचार्य हनुमत् प्रसाद",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_hanumat_prasad.webp",
          phoneNumber = "9868792232",
          email = ""
        ),
        post = "अध्यक्ष",
        priority = 1
      ),
      OrganisationalMember(
        Member(
          name = "आचार्य सतीश",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_satish.webp",
          phoneNumber = "9350945482",
          email = ""
        ),
        post = "महासचिव",
        priority = 2
      ),
      OrganisationalMember(
        Member(
          name = "आर्य जसबीर सिंह",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
          phoneNumber = "9717647455",
          email = ""
        ),
        post = "कोषाध्यक्ष",
        priority = 3
      )
    )
  )
)