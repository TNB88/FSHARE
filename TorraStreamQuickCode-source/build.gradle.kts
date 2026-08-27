version = 1

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}

cloudstream {
    description = "Nhập mã nhanh để cấu hình TorBox hoặc Real-Debrid cho TorraStream và TorraStream-Anime"
    authors = listOf("Bình Pro")
    status = 1
    tvTypes = listOf("Torrent", "Movie", "TvSeries", "Anime")
    requiresResources = true
    language = "vi"
    iconUrl = "https://torrentio.strem.fun/images/logo_v1.png"
}

android {
    namespace = "com.tnb88.torrastreamquickcode"
    buildFeatures {
        buildConfig = true
    }
}
