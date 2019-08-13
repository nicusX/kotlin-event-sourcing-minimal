package eventsourcing.rest

import eventsourcing.readmodel.RecordNotFound
import eventsourcing.readmodel.TrainingClassDTO
import eventsourcing.readmodel.TrainingClassView
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.javaMethod

// FIXME rewrite using Routing DSL and coRouter
@RestController
class TrainingClassReadController(private val view: TrainingClassView) {

    // TODO Return a simplified DTO in the list
    @GetMapping("/classes")
    fun listTrainingClasses() :  ResponseEntity<List<TrainingClassDTO>>
            = ResponseEntity.ok(view.list())

    @GetMapping("/classes/{classId}")
    fun getTrainingClass(@PathVariable classId: String) : ResponseEntity<TrainingClassDTO>
        =  try {
            ResponseEntity.ok(view.getById(classId))
        } catch (e: RecordNotFound) {
            ResponseEntity.notFound().build()
        }


    companion object {
        fun classResourceLocation(classId: String) : URI =
            MvcUriComponentsBuilder.fromMethod(
                    TrainingClassReadController::class.java,
                    TrainingClassReadController::getTrainingClass.javaMethod!!,
                    classId)
                    .build(classId)
    }
}