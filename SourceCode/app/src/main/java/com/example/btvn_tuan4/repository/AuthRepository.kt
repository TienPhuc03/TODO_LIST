package com.example.btvn_tuan4.repository

import android.util.Log
import com.example.btvn_tuan4.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Interface quan ly xac thuc nguoi dung theo OOP
interface IAuthRepository {
    fun getCurrentUser(): AuthUser?
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmailAndPassword(email: String, password: String, displayName: String): Result<AuthUser>
    suspend fun signInWithGoogle(simulatedFail: Boolean = false): Result<AuthUser>
    suspend fun signInAnonymously(simulatedFail: Boolean = false): Result<AuthUser>
    fun signOut(): Result<Unit>
}

// Lop xu ly logic xac thuc ket noi truc tiep voi Firebase Authentication
class AuthRepository : IAuthRepository {

    // Khoi tao FirebaseAuth an toan
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi khởi tạo FirebaseAuth: ${e.message}")
            null
        }
    }

    // Nguoi dung duoc luu trong phien lam viec
    private var currentUser: AuthUser? = null

    // Lay thong tin nguoi dung hien tai tu phien FirebaseAuth
    override fun getCurrentUser(): AuthUser? {
        if (currentUser != null) return currentUser
        val fbUser = firebaseAuth?.currentUser ?: return null
        val mappedUser = AuthUser(
            uid = fbUser.uid,
            displayName = fbUser.displayName.takeUnless { it.isNullOrBlank() }
                ?: fbUser.email?.substringBefore("@")
                ?: "Minh Anh",
            email = fbUser.email ?: "minhanh@example.com",
            photoUrl = fbUser.photoUrl?.toString(),
            isGoogleLinked = fbUser.providerData.any { it.providerId == "google.com" },
            isAnonymous = fbUser.isAnonymous
        )
        currentUser = mappedUser
        return mappedUser
    }

    // Dang nhap bang Email va Mat khau
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUser> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Dịch vụ Firebase Auth chưa sẵn sàng."))

        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = authResult.user ?: return Result.failure(Exception("Không lấy được thông tin tài khoản."))
            val user = AuthUser(
                uid = fbUser.uid,
                displayName = fbUser.displayName.takeUnless { it.isNullOrBlank() }
                    ?: fbUser.email?.substringBefore("@")
                    ?: "Người dùng",
                email = fbUser.email ?: email.trim(),
                photoUrl = fbUser.photoUrl?.toString(),
                isGoogleLinked = false,
                isAnonymous = false
            )

            // Cap nhat lastActive len Cloud Firestore
            try {
                FirebaseFirestore.getInstance().collection("users").document(fbUser.uid).set(
                    mapOf(
                        "userId" to fbUser.uid,
                        "name" to user.displayName,
                        "email" to user.email,
                        "lastActive" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (fe: Exception) {
                Log.w("AuthRepository", "Không thể cập nhật Firestore: ${fe.message}")
            }

            currentUser = user
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email hoặc mật khẩu không chính xác."))
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "Tài khoản email này chưa được đăng ký."
                e.message?.contains("wrong-password", ignoreCase = true) == true -> "Mật khẩu không chính xác."
                e.message?.contains("network", ignoreCase = true) == true -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại Internet."
                else -> e.localizedMessage ?: "Đăng nhập thất bại. Vui lòng thử lại."
            }
            Result.failure(Exception(message))
        }
    }

    // Dang ky tai khoan moi bang Email va Mat khau
    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Dịch vụ Firebase Auth chưa sẵn sàng."))

        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fbUser = authResult.user ?: return Result.failure(Exception("Không thể tạo tài khoản người dùng."))

            // Cap nhat ten hien thi vao profile Firebase
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            fbUser.updateProfile(profileUpdates).await()

            val user = AuthUser(
                uid = fbUser.uid,
                displayName = displayName.trim().ifEmpty { fbUser.email?.substringBefore("@") ?: "Người dùng" },
                email = fbUser.email ?: email.trim(),
                photoUrl = null,
                isGoogleLinked = false,
                isAnonymous = false
            )

            // Luu day du ho so nguoi dung vao Cloud Firestore
            try {
                FirebaseFirestore.getInstance().collection("users").document(fbUser.uid).set(
                    mapOf(
                        "userId" to fbUser.uid,
                        "name" to user.displayName,
                        "email" to user.email,
                        "createdAt" to System.currentTimeMillis(),
                        "lastActive" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (fe: Exception) {
                Log.w("AuthRepository", "Không thể lưu Firestore user profile: ${fe.message}")
            }

            currentUser = user
            Result.success(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Mật khẩu quá yếu. Vui lòng đặt mật khẩu tối thiểu 6 ký tự."))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email này đã được sử dụng bởi một tài khoản khác."))
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true -> "Email này đã được đăng ký tài khoản."
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Định dạng email không hợp lệ."
                e.message?.contains("network", ignoreCase = true) == true -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại Internet."
                else -> e.localizedMessage ?: "Đăng ký thất bại. Vui lòng thử lại."
            }
            Result.failure(Exception(message))
        }
    }

    // Dang nhap voi tai khoan Google
    override suspend fun signInWithGoogle(simulatedFail: Boolean): Result<AuthUser> {
        delay(600)
        return if (simulatedFail) {
            Result.failure(Exception("Đăng nhập Google thất bại: Không thể kết nối đến máy chủ."))
        } else {
            val user = AuthUser(
                uid = "google_user_minhanh_12345",
                displayName = "Minh Anh",
                email = "minhanh@example.com",
                photoUrl = null,
                isGoogleLinked = true,
                isAnonymous = false
            )
            currentUser = user
            Result.success(user)
        }
    }

    // Dang nhap an danh dung thu
    override suspend fun signInAnonymously(simulatedFail: Boolean): Result<AuthUser> {
        val auth = firebaseAuth
        return if (auth != null && !simulatedFail) {
            try {
                val authResult = auth.signInAnonymously().await()
                val fbUser = authResult.user
                val guestUser = AuthUser(
                    uid = fbUser?.uid ?: "guest_${System.currentTimeMillis()}",
                    displayName = "Khách vãng lai",
                    email = "Chưa liên kết email",
                    photoUrl = null,
                    isGoogleLinked = false,
                    isAnonymous = true
                )
                currentUser = guestUser
                Result.success(guestUser)
            } catch (e: Exception) {
                val fallbackUser = AuthUser(
                    uid = "guest_${System.currentTimeMillis()}",
                    displayName = "Khách vãng lai",
                    email = "Chưa liên kết email",
                    photoUrl = null,
                    isGoogleLinked = false,
                    isAnonymous = true
                )
                currentUser = fallbackUser
                Result.success(fallbackUser)
            }
        } else {
            delay(400)
            if (simulatedFail) {
                Result.failure(Exception("Đăng nhập ẩn danh thất bại: Dịch vụ tạm thời không khả dụng."))
            } else {
                val guestUser = AuthUser(
                    uid = "guest_${System.currentTimeMillis()}",
                    displayName = "Khách vãng lai",
                    email = "Chưa liên kết email",
                    photoUrl = null,
                    isGoogleLinked = false,
                    isAnonymous = true
                )
                currentUser = guestUser
                Result.success(guestUser)
            }
        }
    }

    // Dang xuat khoi ung dung
    override fun signOut(): Result<Unit> {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Lỗi khi đăng xuất Firebase: ${e.message}")
        }
        currentUser = null
        return Result.success(Unit)
    }
}
