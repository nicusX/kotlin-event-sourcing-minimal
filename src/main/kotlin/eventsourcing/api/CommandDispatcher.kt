package eventsourcing.api

import arrow.core.Either
import eventsourcing.domain.*
import eventsourcing.domain.handleEnrollStudent
import eventsourcing.domain.handleRegisterNewStudent
import eventsourcing.domain.handleScheduleNewClass
import eventsourcing.domain.handleUnenrollStudent
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Command handler dispatcher
 * This class is doing nothing more than wiring up all command handlers and their dependencies, to be injected
 * into command controllers
 */
class CommandDispatcher(private val classRepo: TrainingClassRepository, private val studentRepo : StudentRepository) {

    val scheduleNewClassHandler: (ScheduleNewClass) -> Either<Problem, ScheduleNewClassSuccess> = handleScheduleNewClass(classRepo)
    val enrollStudentHandler: (EnrollStudent) -> Either<Problem, EnrollStudentSuccess> = handleEnrollStudent(classRepo)
    val unenrollStudentHandler: (UnenrollStudent) -> Either<Problem, UnenrollStudentSuccess> = handleUnenrollStudent(classRepo)
    val registerNewStudentHandler: (RegisterNewStudent) -> Either<Problem, RegisterNewStudentSuccess> = handleRegisterNewStudent(studentRepo)

    // Dispatches
    fun handle(command: Command) : Either<Problem, Success> {
        log.debug("Handing command: {}", command)
        return when(command) {
            is ScheduleNewClass -> scheduleNewClassHandler(command)
            is EnrollStudent -> enrollStudentHandler(command)
            is UnenrollStudent -> unenrollStudentHandler(command)
            is RegisterNewStudent -> registerNewStudentHandler(command)

            else -> throw UnhandledCommandException(command)
        }
    }

    companion object {
        val log : Logger = LoggerFactory.getLogger(CommandDispatcher::class.java)
    }
}

class UnhandledCommandException(command: Command) : Exception("Command ${command::class.simpleName} is not handled")
