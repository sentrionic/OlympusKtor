package xyz.olympusblog.utils

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

private val appConfig = HoconApplicationConfig(ConfigFactory.load())
private val accessKey = appConfig.property("ktor.aws.accessKey").getString()
private val secretAccessKey = appConfig.property("ktor.aws.secretAccessKey").getString()
private val storageBucketName = appConfig.property("ktor.aws.storageBucketName").getString()

const val DIM_MAX = 1080
const val DIM_MIN = 320

object FileUpload {

    private lateinit var client: AmazonS3

    fun init() {
        val credentials = BasicAWSCredentials(accessKey, secretAccessKey)
        client = AmazonS3ClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withRegion(Regions.EU_CENTRAL_1)
            .build()
    }

    fun uploadArticleImage(file: File, directory: String): String {
        val metadata = ObjectMetadata()
        metadata.contentType = "image/jpg"
        val key = "files/$directory.jpg"

        val image = ImageIO.read(file)
        val outputStream = ByteArrayOutputStream()

        if (image.height < DIM_MIN || image.width < DIM_MIN) {
            Thumbnails.of(file).size(DIM_MIN, DIM_MIN).keepAspectRatio(true)
                .toOutputStream(outputStream)
        } else {
            Thumbnails.of(file).size(DIM_MAX, DIM_MAX).keepAspectRatio(true)
                .toOutputStream(outputStream)
        }

        client.putObject(
            storageBucketName,
            key,
            ByteArrayInputStream(outputStream.toByteArray()),
            metadata
        )
        file.delete()
        outputStream.close()
        return "https://$storageBucketName.s3.${client.region}.amazonaws.com/$key"
    }

    fun uploadAvatarImage(file: File, directory: String): String {
        val metadata = ObjectMetadata()
        metadata.contentType = "image/jpg"
        metadata.contentLength = file.length()
        val key = "files/$directory.jpg"

        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(file).size(150, 150).keepAspectRatio(true)
            .toOutputStream(outputStream)

        client.putObject(
            storageBucketName,
            key,
            ByteArrayInputStream(outputStream.toByteArray()),
            metadata
        )
        file.delete()
        outputStream.close()
        return "https://$storageBucketName.s3.${client.region}.amazonaws.com/$key"
    }
}

fun handleMultipartFile(part: PartData.FileItem): File {
    val ext = part.originalFileName?.substringAfterLast('.', "")
    val fileBytes = part.streamProvider().readBytes()
    val file = File("upload-${System.currentTimeMillis()}.$ext")
    file.writeBytes(fileBytes)
    return file
}