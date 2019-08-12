package eventsourcing.eventstore

import com.nhaarman.mockitokotlin2.*
import eventsourcing.EventsAssert.Companion.assertThatEvents
import eventsourcing.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


val AGGREGATE_TYPE : AggregateType = TrainingClassAggregateType
const val AN_AGGREGATE_ID : ClassID = "aggr012"
const val ANOTHER_AGGREGATE_ID : ClassID = "aggr045"


internal class InMemoryEventStoreTest {

    @Test
    fun `given an Event Store containing 3 events of an aggregate, when I retrieve aggregate events, it should return all events, in order, with version 0 to 2`() {

        val sut: EventStore = givenAnInMemoryEventStore (
                { withSavedEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, listOf(
                    StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                    StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                    StudentEnrolled(AN_AGGREGATE_ID, "student-3")
                )) })

        val extractedEvents = sut.getEventsForAggregate(AGGREGATE_TYPE, AN_AGGREGATE_ID)

        val expectedVersionedEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-1", 0),
                StudentUnenrolled(AN_AGGREGATE_ID, "student-2", 1),
                StudentEnrolled(AN_AGGREGATE_ID, "student-3", 2)
        )
        assertThatEvents(extractedEvents).onlyContainsInOrder(expectedVersionedEvents)
    }

    @Test
    fun `given an Event Store containing 3 events of an aggregate, when I store new events specifying version = 2, it should succeed`() {
        val sut : EventStore = givenAnInMemoryEventStore(
                { withSavedEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, listOf(
                        StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                        StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                        StudentEnrolled(AN_AGGREGATE_ID, "student-3")
                )) } )

        val moreEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-4"),
                StudentEnrolled(AN_AGGREGATE_ID, "student-5")
        )

        val aggregateVersion = 2L
        sut.saveEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, moreEvents, aggregateVersion)
    }


    @Test
    fun `given an Event Store containing 3 events of an aggregate, when I store new events specifying a version != 2, it should fail with a ConcurrencyException`() {
        val sut : EventStore = givenAnInMemoryEventStore(
                { withSavedEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, listOf(
                        StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                        StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                        StudentEnrolled(AN_AGGREGATE_ID, "student-3")
                )) } )

        val moreEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-4"),
                StudentEnrolled(AN_AGGREGATE_ID, "student-5")
        )

        val aggregateVersion = 42L
        assertThrows<ConcurrencyException> {
            sut.saveEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, moreEvents, aggregateVersion)
        }
    }



    @Test
    fun `given an Event Store containing events of multiple aggregates, when I retrieve events of one aggregate, it should only retrieve events of the specified aggregate`() {
        val sut : EventStore = givenAnInMemoryEventStore (
                { withSavedEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, listOf(
                        StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                        StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                        StudentEnrolled(AN_AGGREGATE_ID, "student-3")
                )) },
                { withSavedEvents(AGGREGATE_TYPE, ANOTHER_AGGREGATE_ID, listOf(
                        StudentEnrolled(ANOTHER_AGGREGATE_ID, "student-4"),
                        StudentEnrolled(ANOTHER_AGGREGATE_ID, "student-5")
                )) })


        val extractedEvents = sut.getEventsForAggregate(AGGREGATE_TYPE, AN_AGGREGATE_ID)

        val expectedVersionedEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-1", 0),
                StudentUnenrolled(AN_AGGREGATE_ID, "student-2", 1),
                StudentEnrolled(AN_AGGREGATE_ID, "student-3", 2)
        )
        assertThatEvents(extractedEvents).onlyContainsInOrder(expectedVersionedEvents)
    }

    @Test
    fun `given an Event Store only containing events of an aggregate, when I retrieve events for a different aggregate, it should fail with AggregateNotFoundException `() {
        val sut : EventStore = givenAnInMemoryEventStore(
                { withSavedEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, listOf(
                        StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                        StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                        StudentEnrolled(AN_AGGREGATE_ID, "student-3")
                )) } )

        val nonExistingAggregateID = "this-id-does-not-exist"
        assertThrows<AggregateNotFoundException> {
            sut.getEventsForAggregate(AGGREGATE_TYPE, nonExistingAggregateID)
        }
    }

    @Test
    fun `given an empty Event Store, when I store 3 new events of the same aggregate, it should publish all new events with version 0 to 2`() {
        val publisher = mock<EventPublisher<Event>>()
        val sut : EventStore = givenAnInMemoryEventStore(eventPublisher = publisher)

        val newEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-1"),
                StudentUnenrolled(AN_AGGREGATE_ID, "student-2"),
                StudentEnrolled(AN_AGGREGATE_ID, "student-3"))
        sut.saveEvents(AGGREGATE_TYPE, AN_AGGREGATE_ID, newEvents)

        val expectedVersionedEvents = listOf(
                StudentEnrolled(AN_AGGREGATE_ID, "student-1", 0),
                StudentUnenrolled(AN_AGGREGATE_ID, "student-2", 1),
                StudentEnrolled(AN_AGGREGATE_ID, "student-3", 2)
        )
        for(expectedEvent in expectedVersionedEvents)
            verify(publisher).publish(expectedEvent)
        verifyNoMoreInteractions(publisher)
    }
}

private fun givenAnInMemoryEventStore(vararg inits: EventStore.() -> Unit, eventPublisher : EventPublisher<Event> = mock() ) : EventStore {
    val es = InMemoryEventStore(eventPublisher)
    for( init in inits )
        es.init()
    return es
}

private fun EventStore.withSavedEvents(aggregateType: AggregateType, aggregateId: AggregateID, events: Iterable<Event>) {
    saveEvents(aggregateType, aggregateId, events)
}
