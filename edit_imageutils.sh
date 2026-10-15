sed -i 's/    }//g' app/src/main/java/com/example/util/ImageUtils.kt
cat << 'INNER_EOF' >> app/src/main/java/com/example/util/ImageUtils.kt
    fun clearAllImages(context: Context) {
        try {
            val files = context.filesDir.listFiles()
            files?.forEach { file ->
                if (file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
INNER_EOF
