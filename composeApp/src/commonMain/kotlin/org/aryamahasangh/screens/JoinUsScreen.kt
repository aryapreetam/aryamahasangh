@file:OptIn(ExperimentalMaterial3Api::class)

package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationalActivitiesQuery.OrganisationalActivity
import org.aryamahasangh.components.ActivityListItem
import org.aryamahasangh.type.ActivityType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random

val indianStatesToDistricts =
  mapOf(
    "आंध्र प्रदेश" to listOf("अनंतपुर", "चित्तूर", "पूर्व गोदावरी", "गुंटूर", "कडपा", "कृष्णा", "कुरनूल", "नेल्लोर", "प्रकासम", "श्रीकाकुलम", "विशाखापट्टनम", "विजयनगरम", "पश्चिम गोदावरी"),
    "अरुणाचल प्रदेश" to listOf("तवांग", "पश्चिम कामेंग", "पूर्व कामेंग", "पापुम पारे", "कुरुंग कुमे", "क्रा दादी", "लोहित", "निचला सुबनसिरी", "ऊपरी सुबनसिरी", "पश्चिम सियांग", "पूर्व सियांग", "तिरप", "लोंगडिंग", "चांगलांग", "दिबांग घाटी"),
    "असम" to listOf("बक्सा", "बारपेटा", "बोंगाईगांव", "काचार", "चराईदेव", "चिरांग", "दर्रंग", "धेमाजी", "धुबरी", "डिब्रूगढ़", "दीमा हसाओ", "गोलपारा", "हाइलाकांडी", "जोरहाट", "कामरूप", "करिमगंज", "कर्बी आंगलोंग", "कोकराझार", "लखीमपुर", "मरिगांव", "नगांव", "नलबाड़ी", "शिवसागर", "सोनितपुर", "तिनसुकिया", "उदलगुरी"),
    "बिहार" to listOf("अरवल", "औरंगाबाद", "बांका", "बेगूसराय", "भागलपुर", "भोजपुर", "बक्सर", "दरभंगा", "गया", "गोपालगंज", "जमुई", "जहानाबाद", "कैमूर", "कटिहार", "खगड़िया", "किशनगंज", "लखीसराय", "मधेपुरा", "मुंगेर", "मधुबनी", "नालंदा", "नवादा", "पटना", "पूर्णिया", "रोहतास", "सहरसा", "समस्तीपुर", "सारण", "शेखपुरा", "शिवहर", "सीतामढ़ी", "सिवान", "सुपौल", "वैशाली"),
    "छत्तीसगढ़" to listOf("बालोद", "बालोदाबाजार", "बलरामपुर", "बस्तर", "बेमेतरा", "बीजापुर", "बिलासपुर", "दंतेवाड़ा", "धमतरी", "दुर्ग", "गरियाबंद", "जांजगीर-चांपा", "जशपुर", "कबीरधाम", "कांकेर", "कोंडागांव", "कोरबा", "कोरिया", "महासमुंद", "मुंगेली", "नारायणपुर", "रायगढ़", "रायपुर", "सुकमा", "सूरजपुर"),
    "गोवा" to listOf("उत्तर गोवा", "दक्षिण गोवा"),
    "गुजरात" to listOf("अहमदाबाद", "अमरेली", "आणंद", "अरवल्ली", "बनासकांठा", "भरूच", "भावनगर", "बोटाद", "छोटा उदेपुर", "दाहोद", "डांग", "देवभूमि द्वारका", "गांधीनगर", "गिर सोमनाथ", "जामनगर", "जूनागढ़", "खेड़ा", "कच्छ", "महीसागर", "महिसागर", "महेसाणा", "मोरबी", "नर्मदा", "नवसारी", "पंचमहल", "पाटण", "पोरबंदर", "राजकोट", "साबरकांठा", "सुरत", "सुरेंद्रनगर", "तापी", "वडोदरा", "वलसाड"),
    "हरियाणा" to listOf("अंबाला", "भिवानी", "चरखी दादरी", "फरीदाबाद", "फतेहाबाद", "गुरुग्राम", "हिसार", "झज्जर", "जींद", "कैथल", "करनाल", "कुरुक्षेत्र", "महेंद्रगढ़", "मेवात", "पलवल", "पंचकूला", "पानीपत", "रेवाड़ी", "रोहतक", "सिरसा", "सोनीपत", "यमुनानगर"),
    "हिमाचल प्रदेश" to listOf("बिलासपुर", "चंबा", "हमीरपुर", "कांगड़ा", "किन्नौर", "कुल्लू", "लाहौल और स्पीति", "मंडी", "शिमला", "सिरमौर", "सोलन", "उना"),
    "झारखंड" to listOf("बोकारो", "चतरा", "देवघर", "धनबाद", "दुमका", "पूर्वी सिंहभूम", "गढ़वा", "गिरिडीह", "गोड्डा", "गुमला", "हजारीबाग", "जामताड़ा", "खूंटी", "कोडरमा", "लातेहार", "लोहरदगा", "पाकुड़", "पलामू", "रामगढ़", "रांची", "साहिबगंज", "सरायकेला-खरसावां", "सिमडेगा", "पश्चिमी सिंहभूम"),
    "कर्नाटक" to listOf("बागलकोट", "बेंगलुरु ग्रामीण", "बेंगलुरु शहरी", "बेलगावी", "बेल्लारी", "बीदर", "विजयपुरा", "चामराजनगर", "चिकबल्लापुर", "चिकमगलूर", "चित्रदुर्ग", "दक्षिण कन्नड़", "दावणगेरे", "धारवाड़", "गदग", "गुलबर्गा", "हसन", "हावेरी", "कोडगु", "कोलार", "कोप्पल", "मांड्या", "मैसूर", "रायचूर", "रामनगरा", "शिवमोग्गा", "तुमकुर", "उडुपी", "उत्तर कन्नड़", "यादगीर"),
    "केरल" to listOf("अलाप्पुझा", "एर्नाकुलम", "इडुक्की", "कन्नूर", "कासरगोड", "कोल्लम", "कोट्टायम", "कोझिकोड", "मलप्पुरम", "पलक्कड़", "पथनामथिट्टा", "तिरुवनंतपुरम", "तृश्शूर", "वायनाड"),
    "मध्य प्रदेश" to listOf("अलीराजपुर", "अनूपपुर", "अशोकनगर", "बालाघाट", "बड़वानी", "बैतूल", "भिंड", "भोपाल", "बुरहानपुर", "छतरपुर", "छिंदवाड़ा", "दमोह", "दतिया", "देवास", "धार", "डिंडोरी", "गुना", "ग्वालियर", "हरदा", "होशंगाबाद", "इंदौर", "जबलपुर", "झाबुआ", "कटनी", "खंडवा", "खरगोन", "मंडला", "मंदसौर", "मुरैना", "नरसिंहपुर", "नीमच", "पन्ना", "रीवा", "रायसेन", "राजगढ़", "रतलाम", "सागर", "सतना", "सीहोर", "सिवनी", "शहडोल", "शाजापुर", "श्योपुर", "टीकमगढ़", "उज्जैन", "उमरिया", "विदिशा"),
    "महाराष्ट्र" to listOf("अहमदनगर", "अकोला", "अमरावती", "औरंगाबाद", "बीड", "भंडारा", "बुलढाणा", "चंद्रपुर", "धुले", "गडचिरोली", "गोंदिया", "हिंगोली", "जालना", "जालगांव", "कोल्हापुर", "लातूर", "मुंबई", "नागपुर", "नांदेड", "नंदुरबार", "नाशिक", "उस्मानाबाद", "पालघर", "परभणी", "पुणे", "रायगढ़", "रत्नागिरी", "सांगली", "सतारा", "सिंधुदुर्ग", "सोलापुर", "ठाणे", "वर्धा", "वाशिम", "यवतमाल"),
    "मणिपुर" to listOf("बिश्नुपुर", "चंदेल", "चुराचांदपुर", "इंफाल पूर्व", "इंफाल पश्चिम", "जिरीबाम", "काकचिंग", "कामजोंग", "कांगपोकपी", "नोनी", "फेरजॉल", "सेनापति", "तामेंगलोंग", "तेंगनौपाल", "थौबल", "उखरूल"),
    "मेघालय" to listOf("पूर्व गारो हिल्स", "पूर्व खासी हिल्स", "पूर्व जयंतिया हिल्स", "उत्तर गारो हिल्स", "री भोई", "दक्षिण गारो हिल्स", "दक्षिण पश्चिम गारो हिल्स", "दक्षिण खासी हिल्स", "पश्चिम गारो हिल्स", "पश्चिम खासी हिल्स", "पश्चिम जयंतिया हिल्स"),
    "मिजोरम" to listOf("ऐज़ोल", "चम्फाई", "कोलासिब", "लॉंगटलाई", "लुंगलेई", "ममित", "सईहा", "सेरछिप"),
    "नागालैंड" to listOf("दीमापुर", "किफिरे", "कोहिमा", "लोंगलेंग", "मोकोकचुंग", "मोन", "पेरेन", "फेक", "तुएनसांग", "वोखा", "ज़ुनहेबोटो"),
    "ओडिशा" to listOf("अंगुल", "बालांगीर", "बालेश्वर", "बरगढ़", "भद्रक", "बौध", "कटक", "देवगढ़", "ढेंकनाल", "गजपति", "गंजाम", "जगतसिंहपुर", "जाजपुर", "झारसुगुड़ा", "कालाहांडी", "कंधमाल", "केंद्रपाड़ा", "केंदुझर", "खोर्धा", "कोरापुट", "मलकानगिरी", "मयूरभंज", "नबरंगपुर", "नयागढ़", "नुआपाड़ा", "पुरी", "रायगड़ा", "सम्बलपुर", "सोनपुर", "सुंदरगढ़"),
    "पंजाब" to listOf("अमृतसर", "बरनाला", "बठिंडा", "फरीदकोट", "फतेहगढ़ साहिब", "फिरोजपुर", "गुरदासपुर", "होशियारपुर", "जालंधर", "कपूरथला", "लुधियाना", "मानसा", "मोगा", "मुक्तसर", "पठानकोट", "पटियाला", "रूपनगर", "संगरूर", "शहीद भगत सिंह नगर", "तरन तारन"),
    "राजस्थान" to listOf("अजमेर", "अलवर", "बांसवाड़ा", "बारां", "बाड़मेर", "भरतपुर", "भीलवाड़ा", "बीकानेर", "बूंदी", "चित्तौड़गढ़", "चुरू", "दौसा", "धौलपुर", "डूंगरपुर", "हनुमानगढ़", "जयपुर", "जैसलमेर", "जालौर", "झालावाड़", "झुंझुनू", "जोधपुर", "करौली", "कोटा", "नागौर", "पाली", "प्रतापगढ़", "राजसमंद", "सवाई माधोपुर", "सीकर", "सिरोही", "टोंक", "उदयपुर"),
    "सिक्किम" to listOf("पूर्व सिक्किम", "उत्तर सिक्किम", "दक्षिण सिक्किम", "पश्चिम सिक्किम"),
    "तमिलनाडु" to listOf("अरियालूर", "चेन्नई", "कोयंबटूर", "कुड्डालोर", "धर्मपुरी", "डिंडीगुल", "इरोड", "कांचीपुरम", "कन्याकुमारी", "करूर", "कृष्णागिरी", "मदुरै", "नागपट्टिनम", "नामक्कल", "पेरम्बलुर", "पुदुकोट्टई", "रामनाथपुरम", "सेलम", "शिवगंगा", "तंजावुर", "तिरुचिरापल्ली", "तिरुनेलवेली", "तिरुप्पुर", "तिरुवल्लुर", "तिरुवन्नामलाई", "तिरुवरुर", "तूतुकुड़ी", "वेल्लोर", "विलुप्पुरम", "विरुधुनगर"),
    "तेलंगाना" to listOf("आदिलाबाद", "भद्राद्री कोठागुडेम", "हैदराबाद", "जगित्याल", "जंगाओन", "जयशंकर भूपालपल्ली", "जोगुलाम्बा गडवाल", "कामारेड्डी", "करीमनगर", "खम्मम", "कोमाराम भीम", "महबूबाबाद", "महबूबनगर", "मंचेरियल", "मेडक", "मेडचल-मलकाजगिरि", "मुलुगु", "नगरकरनूल", "नलगोंडा", "निर्मल", "निजामाबाद", "पेद्दापल्ली", "राजन्ना सिरसिल्ला", "रंगारेड्डी", "संगारेड्डी", "सिद्दिपेट", "सूर्यापेट", "विकाराबाद", "वानापर्ती", "वारंगल", "यादाद्री भुवनगिरी"),
    "त्रिपुरा" to listOf("धलाई", "गोमती", "खोवाई", "उत्तर त्रिपुरा", "सेपाहीजाला", "दक्षिण त्रिपुरा", "पश्चिम त्रिपुरा"),
    "उत्तर प्रदेश" to listOf("आगरा", "अलीगढ़", "अम्बेडकर नगर", "अमेठी", "अमरोहा", "औरैया", "आजमगढ़", "बागपत", "बहराइच", "बलिया", "बलरामपुर", "बांदा", "बाराबंकी", "बरेली", "बस्ती", "बिजनौर", "बुलंदशहर", "चंदौली", "चित्रकूट", "देवरिया", "एटा", "इटावा", "फैजाबाद", "फर्रुखाबाद", "फतेहपुर", "फिरोजाबाद", "गौतम बुद्ध नगर", "गाजियाबाद", "गाजीपुर", "गोंडा", "गोरखपुर", "हमीरपुर", "हापुड़", "हरदोई", "हाथरस", "जालौन", "जौनपुर", "झांसी", "कन्नौज", "कानपुर देहात", "कानपुर नगर", "कासगंज", "कौशाम्बी", "खीरी", "ललितपुर", "लखनऊ", "महाराजगंज", "महोबा", "मैनपुरी", "मथुरा", "मऊ", "मेरठ", "मिर्जापुर", "मुरादाबाद", "मुजफ्फरनगर", "पीलीभीत", "प्रतापगढ़", "रायबरेली", "रामपुर", "सहारनपुर", "संभल", "संत कबीर नगर", "संत रविदास नगर", "शाहजहांपुर", "शामली", "श्रावस्ती", "सिद्धार्थनगर", "सीतापुर", "सोनभद्र", "सुल्तानपुर", "उन्नाव", "वाराणसी"),
    "उत्तराखंड" to listOf("अल्मोड़ा", "बागेश्वर", "चमोली", "चंपावत", "देहरादून", "हरिद्वार", "नैनीताल", "पौड़ी गढ़वाल", "पिथौरागढ़", "रुद्रप्रयाग", "टिहरी गढ़वाल", "उधम सिंह नगर", "उत्तरकाशी"),
    "पश्चिम बंगाल" to listOf("अलीपुरद्वार", "बांकुड़ा", "बीरभूम", "कोचबिहार", "दार्जिलिंग", "हुगली", "हावड़ा", "जलपाईगुड़ी", "झारग्राम", "कलकत्ता", "मालदा", "मुर्शिदाबाद", "नदिया", "उत्तर २४ परगना", "पश्चिम मेदिनीपुर", "पुरुलिया", "दक्षिण २४ परगना", "उत्तर दिनाजपुर", "दक्षिण दिनाजपुर")
  )


@Composable
@Preview
fun JoinUsScreen() {
    Column(modifier = Modifier.padding(8.dp)){
      Text("नमस्ते जी,\n" +
          "आप निर्मात्री सभा द्वारा आयोजित दो दिवसीय लघु गुरुकुल पाठ्यक्रम पूर्ण कर आर्य महासंघ से जुड़ सकते है। \n" +
          "निचे आप अपना क्षेत्र चुनकर आपके क्षेत्रों में आयोजित होने वाले सत्रों के विवरण देख सकते है। ")

      ActivityForm()
    }
}


@Composable
fun ActivityForm() {
  var selectedState by remember { mutableStateOf<String?>(null) }
  var selectedDistrict by remember { mutableStateOf<String?>(null) }
  var activities by remember { mutableStateOf<List<OrganisationalActivity>>(emptyList()) }

  // Reset district on state change
  LaunchedEffect(selectedState) {
    selectedDistrict = null
  }

  val showActivitiesEnabled = selectedState != null

  Column(modifier = Modifier.padding(top = 8.dp).width(500.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StateDropdown(
        states = indianStatesToDistricts.keys.toList(),
        selectedState = selectedState,
        onStateSelected = { selectedState = it },
        modifier = Modifier.weight(1f)
      )
      // District Selection (Conditional)
      val districts = indianStatesToDistricts[selectedState] ?: emptyList()
      DistrictDropdown(
        districts = districts,
        selectedDistrict = selectedDistrict,
        onDistrictSelected = { selectedDistrict = it },
        modifier = Modifier.weight(1f)
      )
    }

    // Show Activities Button
    Button(
      onClick = {
        // Simulate fetching activities (Replace with your API call)
        activities = fetchActivities(selectedState, selectedDistrict)
      },
      enabled = showActivitiesEnabled,
      modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, bottom = 8.dp)
    ) {
      Text("आगामी सत्र दिखाएं")
    }

    // Activities List
    if (activities.isNotEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f) // Limits height to remaining space
      ) {
        ActivitiesList(activities = activities)
      }
    }
  }
}

@Composable
fun StateDropdown(states: List<String>, selectedState: String?, onStateSelected: (String) -> Unit, modifier: Modifier = Modifier) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier = modifier) {
    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        value = selectedState ?: "राज्य चुनें",
        label = { Text("राज्य") },
        onValueChange = {},
        placeholder = { Text("Color") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.textFieldColors(),
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        states.forEach { selectionOption ->
          key(selectionOption) {
            DropdownMenuItem(
              text = { Text(selectionOption) },
              onClick = {
                onStateSelected(selectionOption)
                expanded = false
              },
              contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
          }
        }
      }
    }
//    OutlinedTextField(
//      readOnly = true,
//      value = selectedState ?: "Select State",
//      onValueChange = { },
//      label = { Text("State") },
//      trailingIcon = {
//        ExposedDropdownMenuDefaults.TrailingIcon(
//          expanded = expanded
//        )
//      },
//      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
//      enabled = false // to display as a dropdown
//    )
//    DropdownMenu(
//      expanded = expanded,
//      onDismissRequest = { expanded = false },
//    ) {
//      states.forEach { state ->
//        DropdownMenuItem(
//          text = { Text(state) },
//          onClick = {
//            onStateSelected(state)
//            expanded = false
//          },
//          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
//        )
//      }
//    }
  }
}

@Composable
fun DistrictDropdown(districts: List<String>, selectedDistrict: String?, onDistrictSelected: (String?) -> Unit, modifier: Modifier = Modifier) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        value = selectedDistrict ?: "जनपद चुनें (वैकल्पिक)",
        label = { Text("जनपद") },
        onValueChange = {
        },
        placeholder = { Text("Color") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.textFieldColors(),
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        DropdownMenuItem(
          text = { Text("None") },
          onClick = {
            onDistrictSelected(null)
            expanded = false
          }
        )
        districts.forEach { selectionOption ->
          key(selectionOption) {
            DropdownMenuItem(
              text = { Text(selectionOption) },
              onClick = {
                onDistrictSelected(selectionOption)
                expanded = false
              },
              contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
          }
        }
      }
    }

//    OutlinedTextField(
//      readOnly = true,
//      value = selectedDistrict ?: "Select District (Optional)",
//      onValueChange = { },
//      label = { Text("District") },
//      trailingIcon = {
//        ExposedDropdownMenuDefaults.TrailingIcon(
//          expanded = expanded
//        )
//      },
//      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
//      enabled = false // To display as a dropdown
//    )

//    DropdownMenu(
//      expanded = expanded,
//      onDismissRequest = { expanded = false },
//    ) {
//      DropdownMenuItem(
//        text = { Text("None") },
//        onClick = {
//          onDistrictSelected(null)
//          expanded = false
//        }
//      )
//      districts.forEach { district ->
//        DropdownMenuItem(
//          text = { Text(district) },
//          onClick = {
//            onDistrictSelected(district)
//            expanded = false
//          }
//        )
//      }
//    }
  }
}

@Preview
@Composable
fun ActivityListPreview(){
  ActivitiesList(fetchActivities("Maharashtra", "Mumbai"))
}

@Composable
fun ActivitiesList(activities: List<OrganisationalActivity>) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(activities){
      ActivityListItem(it)
      VerticalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
  }
}

// Helper Functions

val listOfCities = "फरीदाबाद, गुड़गांव, मेवात, रोहतक, सोनीपत, रेवाड़ी, झज्जर, पानीपत, पलवल, महेंद्रगढ़, भिवानी,जींद, मेरठ, गाजियाबाद, गौतम बुद्ध नगर (नोएडा), ग्रेटर नोएडा, बुलंदशहर, बागपत, हापुड़, मुजफ्फरनगर"

val placeDesc = listOf("संत भवन", "आर्य समाज", "शहर उद्यान", "गुरूकुल प्रांगण", "सामाजिक भवन")

fun fetchActivities(state: String?, district: String?): List<OrganisationalActivity> {
  // Simulate fetching activities from a data source
  // Replace this with your actual API call or data retrieval logic
  val filteredActivities = mutableListOf<OrganisationalActivity>()
  val allActivities = listOf(
    OrganisationalActivity(
      id = "1",
      name = "आर्य प्रशिक्षण सत्र",
      description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
      startDateTime = "2025-05-10T14:00:00",
      endDateTime = "2025-05-10T15:00:00",
      activityType = ActivityType.SESSION,
      place = "${placeDesc.random()}, ${listOfCities.split(",").random()}"
    )
    ,
      OrganisationalActivity(
        id = "2",
        name = "आर्या प्रशिक्षण सत्र",
        description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
        startDateTime = "2025-04-22T09:00:00",
        endDateTime = "2025-04-22T12:00:00",
        activityType = ActivityType.SESSION,
        place = "${placeDesc.random()}, ${listOfCities.split(",").random()}"
      )
    ,
      OrganisationalActivity(
        id = "3",
        name = "आर्या प्रशिक्षण सत्र",
        description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
        startDateTime = "2025-05-10T14:00:00",
        endDateTime = "2025-05-10T15:00:00",
        activityType = ActivityType.SESSION,
        place = "${placeDesc.random()}, ${listOfCities.split(",").random()}"
    ),
      OrganisationalActivity(
        id = "4",
        name = "आर्य प्रशिक्षण सत्र",
        description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
        startDateTime = "2025-02-20T15:38:26.337446",
        endDateTime = "2025-04-08T15:38:26.337446",
        activityType = ActivityType.SESSION,
        place = "${placeDesc.random()}, ${listOfCities.split(",").random()}"
    )
  )

  //Filter activities according to state
  val stateActivities = allActivities.filter {
      organisationalActivity ->
    organisationalActivity.place.contains(state ?: "", ignoreCase = true)
  }

  //Then filter further with district if needed
  if(district != null){
    stateActivities.filter {
        organisationalActivity ->
      organisationalActivity.place.contains(district, ignoreCase = true)
    }
  }
  else{
    return stateActivities;
  }


  return allActivities.take(Random.nextInt(1,allActivities.size))
}

@Preview
@Composable
fun PreviewActivityForm() {
  MaterialTheme {
    ActivityForm()
  }
}
