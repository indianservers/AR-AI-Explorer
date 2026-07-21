package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog

object CrossSubjectJourneyCatalog {
    val journeys=listOf(
        CrossSubjectJourney("waves","Waves",listOf(JourneyStop(SchoolSubject.MATHEMATICS,"math-coordinate-geometry","Graphs reveal repeating change.","math-fractions",8),JourneyStop(SchoolSubject.PHYSICS,"physics-wave-speed","Frequency and wavelength describe mechanical waves.","physics-velocity-acceleration",12),JourneyStop(SchoolSubject.BIOLOGY,"biology-cell-structure","Vibration is converted into signals by specialised structures.",null,8))),
        CrossSubjectJourney("diffusion","Diffusion",listOf(JourneyStop(SchoolSubject.MATHEMATICS,"math-probability","Random movement creates aggregate patterns.","math-fractions",8),JourneyStop(SchoolSubject.CHEMISTRY,"chemistry-ions-isotopes","Particles move and spread.",null,10),JourneyStop(SchoolSubject.BIOLOGY,"biology-diffusion-osmosis","Membranes make diffusion biologically selective.","biology-cell-structure",12))),
        CrossSubjectJourney("optics","Optics",listOf(JourneyStop(SchoolSubject.MATHEMATICS,"math-triangles","Angles constrain rays.","math-fractions",8),JourneyStop(SchoolSubject.PHYSICS,"physics-wave-speed","Light shares wave relationships while using a different model.",null,12),JourneyStop(SchoolSubject.BIOLOGY,"biology-cell-structure","Eye structures focus and detect light.",null,10))),
        CrossSubjectJourney("electricity","Electricity",listOf(JourneyStop(SchoolSubject.PHYSICS,"physics-electric-circuits","Potential difference drives charge flow.",null,12),JourneyStop(SchoolSubject.CHEMISTRY,"chemistry-ions-isotopes","Ion movement links chemistry to current.","chemistry-atomic-number-mass-number",10),JourneyStop(SchoolSubject.BIOLOGY,"biology-cell-structure","Ion gradients support electrical signalling.",null,10))),
        CrossSubjectJourney("energy","Energy",listOf(JourneyStop(SchoolSubject.PHYSICS,"physics-newton-second-law","Forces transfer energy through motion.","physics-velocity-acceleration",12),JourneyStop(SchoolSubject.CHEMISTRY,"chemistry-chemical-bonding","Bond changes redistribute energy.","chemistry-periodic-trends",12),JourneyStop(SchoolSubject.BIOLOGY,"biology-diffusion-osmosis","Cells couple energy release to transport and work.","biology-cell-structure",10)))
    )
}
class CrossSubjectJourneyEngine {
    fun open(journey:CrossSubjectJourney,start:Int=0)=JourneyPosition(journey.id,start.coerceIn(journey.stops.indices),null)
    fun move(journey:CrossSubjectJourney,position:JourneyPosition,destination:Int):JourneyPosition{require(destination in journey.stops.indices);return JourneyPosition(journey.id,destination,position.stopIndex)}
    fun returnToPrevious(position:JourneyPosition)=position.returnStopIndex?.let{JourneyPosition(position.journeyId,it,null)}?:position
    fun preservesOwnership(journey:CrossSubjectJourney)=journey.stops.all{stop->LearningIntelligenceCatalog.concepts.singleOrNull{it.conceptId==stop.conceptId}?.subject==stop.subject}
}
