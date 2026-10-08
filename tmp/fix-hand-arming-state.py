from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/intelligence/HandIntelligenceEngine.kt';s=p.read_text(encoding='utf-8')
s=s.replace('''                }
            } else candidate=null
            val inspect=''', '''                }
                val c=candidateTarget!!
                return result(if(c.region.precision>.7) HandIntent.PRECISION_GRAB else HandIntent.GRAB,InteractionPhase.IDLE,hand,confidence,SpatialTarget(c.objectId,c.region,c.position,c.finalScore))
            } else candidate=null
            val inspect=''',1)
s=s.replace('machine.set(if(target!=null) InteractionState.HOVER else InteractionState.IDLE)', 'machine.set(if(target==null) InteractionState.IDLE else if(hand.motion.velocity.magnitude()>.15) InteractionState.APPROACH else if((candidateTarget?.proximityScore ?: 0f)>.8f) InteractionState.CONTACT else InteractionState.HOVER)')
s=s.replace('        } else releasingAt=null\n        val other=', '        } else releasingAt=null\n        if(maxOf(hand.pose.pinchStrength,hand.pose.grabStrength)<InteractionThresholds.GRAB_CONTINUE) return result(HandIntent.GRAB,InteractionPhase.IDLE,hand,.45f)\n        val other=')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/test/java/com/indianservers/aiexplorer/handintelligence/HandIntelligenceTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}');s=s[:i]+'''    @Test fun armingReportsGrabReadyAndDoesNotCommitAnEarlyContact() { val e=HandIntelligenceEngine();val state=e.process(listOf(hand()),scene(),0);assertEquals(InteractionState.GRAB_READY,state.state);assertFalse(state.targetLocked);assertEquals(InteractionPhase.IDLE,state.phase) }
'''+s[i:];p.write_text(s,encoding='utf-8')
