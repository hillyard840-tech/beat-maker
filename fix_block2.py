import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Replace multiple ).build() or broken ones
content = re.sub(
    r'private val db = Room\.databaseBuilder\([\s\S]*?AppDatabase::class\.java, "chat-database"[\s\S]*?\.build\(\)',
    'private val db = Room.databaseBuilder(\n        application,\n        AppDatabase::class.java, "chat-database"\n    ).fallbackToDestructiveMigration().build()',
    content
)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
