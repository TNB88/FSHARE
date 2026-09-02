version = 1

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}

cloudstream {
    description = "TorBox Việt: nội dung tiếng Việt, chọn nguồn 4K/1080p rõ ràng và nhập mã nhanh"
    authors = listOf("Bình Pro")
    status = 1
    tvTypes = listOf("Movie", "TvSeries", "Anime", "Torrent")
    requiresResources = true
    language = "vi"
    iconUrl = "https://torrentio.strem.fun/images/logo_v1.png"
}

android {
    namespace = "com.tnb88.torboxprovider"
    buildFeatures { buildConfig = true }
}
