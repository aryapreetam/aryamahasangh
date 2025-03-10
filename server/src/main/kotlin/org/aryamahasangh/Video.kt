package org.aryamahasangh

import com.expediagroup.graphql.generator.scalars.ID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Video(val id: ID, val title: String, val description: String, val url: String, val thumbnailUrl: String, val videoId: String)

@OptIn(ExperimentalUuidApi::class)
val videosList: List<Video> = listOf(
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १",
    description = "ईश्वर के नाम की व्याख्या भाग 1",
    url = "https://www.youtube.com/watch?v=VRV-DbnKekM",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_1.webp",
    videoId = "VRV-DbnKekM"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग २",
    description = "ईश्वर के नाम की व्याख्या भाग 2",
    url = "https://youtu.be/rRwCUgkLVd0?si=6C009TfX_rquQ04i",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_2.webp",
    videoId = "rRwCUgkLVd0"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ३",
    description = "ईश्वर के नाम की व्याख्या भाग 3",
    url = "https://youtu.be/cy6K36_OIUM?si=9qMC8y5FOUnlH7EK",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_3.webp",
    videoId = "cy6K36_OIUM"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ४",
    description = "ईश्वर के नाम की व्याख्या भाग 4",
    url = "https://youtu.be/sEcX__iDeYY?si=r-MS2pq07OXohRNq",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_4.webp",
    videoId = "sEcX__iDeYY"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ५",
    description = "ईश्वर के नाम की व्याख्या भाग 5",
    url = "https://youtu.be/bctMktlkK_o?si=0QI7PHohOeTgQn_h",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_5.webp",
    videoId = "bctMktlkK_o"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ६",
    description = "ईश्वर के नाम की व्याख्या भाग 6",
    url = "https://youtu.be/dvbRLdr-7u8?si=rfwUz0aKF8sBpx-e",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_6.webp",
    videoId = "dvbRLdr-7u8"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ७",
    description = "ईश्वर के नाम की व्याख्या भाग 7",
    url = "https://youtu.be/fVwaRGcEro0?si=81TL202kVH1nVMZX",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_7.webp",
    videoId = "fVwaRGcEro0"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ८",
    description = "ईश्वर के नाम की व्याख्या भाग 8",
    url = "https://youtu.be/NEKbz6dx-70?si=xxejuZkGARNE5-Hf",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_8.webp",
    videoId = "NEKbz6dx-70"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ९",
    description = "ईश्वर के नाम की व्याख्या भाग 9",
    url = "https://youtu.be/soEbIFT29TM?si=r-cHEaW8u6QGjfae",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_9.webp",
    videoId = "soEbIFT29TM"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १०",
    description = "ईश्वर के नाम की व्याख्या भाग 10",
    url = "https://youtu.be/72Cwf248Qr4?si=aD2s3b8OPoTC72Bu",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_10.webp",
    videoId = "72Cwf248Qr4"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग ११",
    description = "ईश्वर के नाम की व्याख्या भाग 11",
    url = "https://youtu.be/NqRqziicZ60?si=BNJ_yOGikTicaWc0",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_11.webp",
    videoId = "NqRqziicZ60"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १२",
    description = "ईश्वर के नाम की व्याख्या भाग 12",
    url = "https://youtu.be/XECGokLT14I?si=BC7J8YeemCk04-J-",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_12.webp",
    videoId = "XECGokLT14I"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १३",
    description = "ईश्वर के नाम की व्याख्या भाग 13",
    url = "https://youtu.be/Gi4sJr8HC1U?si=IO39Q78eXew8AIvW",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_13.webp",
    videoId = "Gi4sJr8HC1U"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १४",
    description = "ईश्वर के नाम की व्याख्या भाग 14",
    url = "https://youtu.be/IAxmzH9RWnY?si=ewV6JhLGeFq7b3BI",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_14.webp",
    videoId = "IAxmzH9RWnY"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १५",
    description = "ईश्वर के नाम की व्याख्या भाग 15",
    url = "https://youtu.be/G7zuARqZqV4?si=VsdQ4-z_MGSgm5i1",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_15.webp",
    videoId = "G7zuARqZqV4"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १६",
    description = "ईश्वर के नाम की व्याख्या भाग 16",
    url = "https://youtu.be/0ZHfmjXOsU4?si=co5FbH5oC49w4s-v",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_16.webp",
    videoId = "0ZHfmjXOsU4"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १७",
    description = "ईश्वर के नाम की व्याख्या भाग 17",
    url = "https://youtu.be/hiz91U2Pdrc?si=McnwMQ__jHPnSxIL",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_17.webp",
    videoId = "hiz91U2Pdrc"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १८",
    description = "ईश्वर के नाम की व्याख्या भाग 18",
    url = "https://youtu.be/ik3LGaLZrjU?si=1dTIqgfctdr7LvpL",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_18.webp",
    videoId = "ik3LGaLZrjU"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग १९",
    description = "ईश्वर के नाम की व्याख्या भाग 19",
    url = "https://youtu.be/LM6y7FPHzM8?si=p4x524U0F0FKnKqy",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_19.webp",
    videoId = "LM6y7FPHzM8"
  ),
  Video(
    id = ID(Uuid.random().toString()),
    title = "सत्यार्थ प्रकाश व्याख्या भाग २०",
    description = "ईश्वर के नाम की व्याख्या भाग 20",
    url = "https://youtu.be/6lm_5-qHcJw?si=SZ8DYiS88gADAOt-",
    thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//sp_20.webp",
    videoId = "6lm_5-qHcJw"
  ),
)