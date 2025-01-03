This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop, Server.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here too.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [GitHub](https://github.com/JetBrains/compose-multiplatform/issues).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

## Todo

### Buidld pages for following

छात्र सभा
----------------
छात्र सभा का उद्देश्य आर्य संतति अर्थात विश्व भर के आर्य परिवार के बालक बालिकाओं को सही समय पर आर्यत्व में स्थापित करना है जिससे आर्य परिवार की भावी पीढ़ी उच्च आधुनिक शिक्षा के साथ- साथ अपनी वैदिक संस्कृति, श्रेष्ठ परंपराओं, मानव मूल्यों से युक्त स्वस्थ व सबल बने

छात्र सभा का मुख्य उद्देश्य:

1. आधुनिक शिक्षा/modern education: आर्य परिवार के बालक बालिका न्यूनतम स्नातक तक की शिक्षा अवश्य लेना सुनिश्चित करना व उच्च से उच्च शिक्षा ग्रहण करने के लिए प्रोत्साहित करना

2. स्वास्थ्य रक्षा/health defence: आर्य परिवार का बालक बालिकाएं उत्तम स्वास्थ्य के लिए उचित खान-पान, व्यायाम, दिनचर्या ऋतुचार्य आदि का महत्व ज्ञान व व्यवहारिक अभ्यास उपलब्ध कराना

3. आत्मरक्षा/Self defence: आपात स्थितियां ,विपरीत परिस्थितियों, आवश्यकता पड़ने पर अपनी अपने परिवार आर्य संगठन व राष्ट्र रक्षा कर सकने के योग्य बनाना व परस्पर सहयोग करना

4. नैतिकता की रक्षा/moral defence: प्रत्येक आर्य परिवार के बालक बालिकाओं की आज के परिवेश में ग्लोबलाइजेशन आधुनिकता आदि के नाम पर नैतिक पतन को रोकना व उनके आर्यत्व को बचाए रखने, बनाए रखने में समर्थ करना

5. counselling and guidance: आर्य छात्रों को पर्सनल तथा करियर counselling उपलब्ध कराना

राष्ट्रीय आर्य दलितोद्धारिणी सभा
----------------------
सभा का उद्देश्य
*हजारों वर्षों से शोषित और पीड़ित एक बहुत बड़ा समुदाय जिसको समाज से अलग-थलग कर दिया गया जो कभी आर्यों का एक भाग होता था और उसको भी वही सम्मान अधिकार प्राप्त था जो ब्राह्मण क्षत्रिय वैश्य को था आज उसको अलग कर दिया गया। उसी के उद्धार के लिए राष्ट्रीय दलितोद्धारिणी सभा बनाई गई है ताकि प्रत्येक मनुष्य समानता का अधिकार प्राप्त कर सके। *

वानप्रस्थ आयोग
---------------
ब्रह्मचर्याश्रमं समाप्य गृही भवेद् गृही भूत्वा वनी भवेद्

