# Mikhmon Native (المرحلة 2)

تطبيق أندرويد **أصلي (Kotlin)** يتصل مباشرة بجهاز MikroTik عبر بروتوكول **RouterOS API**،
بدون الحاجة لسيرفر PHP أو ويب — على عكس Mikhmon الأصلي الذي هو تطبيق ويب PHP.

---

## ✅ ما يعمل حالياً في هذه النسخة (Phase 2)

- تسجيل دخول مباشر لجهاز MikroTik (يدعم API العادي 8728 و API-SSL المشفّر 8729).
- حفظ عدة أجهزة MikroTik والتبديل بينها.
- لوحة تحكم: اسم الجهاز (Identity)، إصدار RouterOS، مدة التشغيل، حمل المعالج، الذاكرة الحرة،
  وعدد مستخدمي Hotspot المتصلين حالياً (تحديث بالسحب للأسفل).
- إدارة مستخدمي Hotspot: عرض القائمة، إضافة مستخدم جديد (مع الباقة/Profile)، حذف مستخدم.
- **الكروت (Vouchers):** توليد دفعة كروت بأسلوب Mikhmon (اسم المستخدم = كلمة المرور = رمز عشوائي)
  مرتبطة بباقة محددة، مع إمكانية الطباعة أو المشاركة عبر نظام الطباعة في أندرويد.
- **إدارة الباقات (Profiles):** عرض/إضافة/حذف باقات Hotspot (حد السرعة، مدة الجلسة، عدد الأجهزة المشتركة).
- **التقارير:** إجمالي استهلاك البيانات لكل المستخدمين، وترتيب أعلى 20 مستخدم استهلاكاً مع
  شريط نسبي لكل واحد (بالاعتماد على عدادات bytes-in/bytes-out التراكمية في الراوتر).
- **النسخ الاحتياطي:** إنشاء نسخة احتياطية على الراوتر نفسه (`/system/backup/save`) وعرض
  قائمة ملفات `.backup` الموجودة عليه.

## 🚧 غير موجود بعد (يحتاج مراحل قادمة)

- تنزيل ملف النسخة الاحتياطية الثنائي مباشرة إلى الهاتف (يحتاج FTP/SFTP، غير مدعوم عبر
  RouterOS API القياسي بشكل مباشر — راجع قسم "ملاحظة عن النسخ الاحتياطي" أدناه).
- تعديل باقة موجودة (متاح حالياً: عرض، إضافة، حذف فقط — لا تعديل).
- دعم عدة مسؤولين (Admin levels) كما في Mikhmon.
- رسوم بيانية تفاعلية (خطوط/دوائر) بدل الأشرطة النسبية الحالية.

هذا الهيكل مبني بحيث يسهل إضافة أي من هذه الميزات لاحقاً — كل ميزة هي ببساطة أمر جديد
عبر نفس `RouterOsApi.talk(listOf("/command", "=param=value"))`.

---

## 🛠️ كيف تبني ملف APK فعلي

هذا المشروع مُعدّ للبناء تلقائياً عبر **GitHub Actions** (ملف `.github/workflows/build.yml`):
كل رفعة (push) لفرع `main` تُنتج تلقائياً ملف APK جاهزاً للتحميل من تبويب **Actions** في
مستودع GitHub الخاص بك (قسم Artifacts).

للبناء يدوياً بدلاً من ذلك عبر Android Studio:
1. نزّل [Android Studio](https://developer.android.com/studio) (مجاني).
2. افتح Android Studio → `Open` → اختر مجلد `MikhmonNative`.
3. اضغط "Trust Project" إذا طُلب منك، وانتظر انتهاء "Gradle Sync" تلقائياً.
4. من القائمة: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`.
5. ستجد ملف الـ APK داخل: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 ملاحظات أمنية مهمة (اتصال عبر الإنترنت)

1. **استخدم API-SSL (منفذ 8729) وليس API العادي (8728)** عند الاتصال عبر الإنترنت.
   على الراوتر:
   ```
   /ip service set api-ssl disabled=no port=8729 certificate=your-cert
   /ip service set api disabled=yes
   ```
2. **لا تفتح المنفذ لكل الإنترنت** — قيّده بعنوان IP ثابت إن أمكن عبر Firewall:
   ```
   /ip firewall filter add chain=input protocol=tcp dst-port=8729 src-address=YOUR_IP action=accept
   /ip firewall filter add chain=input protocol=tcp dst-port=8729 action=drop
   ```
3. **الأفضل من فتح API للإنترنت مباشرة: استخدام VPN** (WireGuard مدمج في RouterOS الحديث).
4. لا تستخدم المستخدم `admin` الافتراضي — أنشئ مستخداً مخصصاً بصلاحيات محدودة لهذا التطبيق فقط.

## 📦 ملاحظة عن النسخ الاحتياطي

`/system/backup/save` عبر الـ API ينشئ الملف على تخزين الراوتر الداخلي فقط. RouterOS API
لا يوفر طريقة قياسية لسحب محتوى ملف ثنائي كامل عبر نفس البروتوكول. لتنزيل الملف فعلياً
إلى جهاز آخر تحتاج أحد هذه الطرق من خارج هذا التطبيق: WinBox (سحب وإفلات من نافذة Files)،
أو تفعيل FTP/SFTP على الراوتر ثم استخدام تطبيق يدعمه.

---

## 🧩 البنية التقنية

- `api/RouterOsApi.kt` — تطبيق كامل لبروتوكول RouterOS API الثنائي (ترميز الأطوال، الجمل، تسجيل الدخول الحديث).
- `api/ServerConfig.kt` / `Session.kt` — حفظ وإدارة إعدادات الأجهزة المتصلة.
- `ui/LoginActivity`, `DashboardActivity` — تسجيل الدخول ولوحة التحكم الرئيسية.
- `ui/UsersActivity`, `AddUserActivity` — إدارة مستخدمي Hotspot.
- `ui/ProfilesActivity`, `AddProfileActivity` — إدارة الباقات.
- `ui/VouchersActivity` — توليد وطباعة الكروت.
- `ui/ReportsActivity` — تقارير الاستهلاك.
- `ui/BackupActivity` — النسخ الاحتياطي على الراوتر.

كل الاتصال بالشبكة يتم على `Dispatchers.IO` عبر Kotlin Coroutines (لا يُجمّد الواجهة أبداً).
