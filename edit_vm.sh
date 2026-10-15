sed -i 's/fun clearHistory() {/fun clearHistory(context: android.content.Context) {/g' app/src/main/java/com/example/ui/ChatViewModel.kt
sed -i 's/repository.clear()/repository.clear()\n            ImageUtils.clearAllImages(context)/g' app/src/main/java/com/example/ui/ChatViewModel.kt
