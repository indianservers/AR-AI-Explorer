package com.indianservers.aiexplorer.learningintelligence.validation

import com.indianservers.aiexplorer.learningintelligence.model.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

sealed interface ValidationRequest {
    data class Numeric(val expected:Double,val actual:String,val tolerance:Double=1e-6):ValidationRequest
    data class Fraction(val expectedNumerator:Int,val expectedDenominator:Int,val actual:String):ValidationRequest
    data class Algebraic(val expected:String,val actual:String,val variable:String="x"):ValidationRequest
    data class OrderedSteps(val expected:List<String>,val actual:List<String>):ValidationRequest
    data class GraphCharacteristics(val expected:Map<String,String>,val actual:Map<String,String>):ValidationRequest
    data class Physics(val expected:Double,val actualValue:String,val requiredUnit:String,val actualUnit:String?,val tolerance:Double=1e-6,val plausibleRange:ClosedFloatingPointRange<Double>?=null,val requiredSign:Int?=null,val requiredSignificantFigures:Int?=null,val expectedDirection:String?=null,val actualDirection:String?=null):ValidationRequest
    data class ChemicalEquation(val reactants:Map<String,Int>,val products:Map<String,Int>,val reactantCharge:Int=0,val productCharge:Int=0):ValidationRequest
    data class MoleRatio(val expected:List<Int>,val actual:String):ValidationRequest
    data class FormulaWriting(val expectedElements:Map<String,Int>,val actualElements:Map<String,Int>,val expectedCharge:Int=0,val actualCharge:Int=0):ValidationRequest
    data class PH(val expected:Double,val actual:String,val tolerance:Double=.02):ValidationRequest
    data class Labels(val expected:Set<String>,val actual:Set<String>):ValidationRequest
    data class Sequence(val expected:List<String>,val actual:List<String>):ValidationRequest
    data class Classification(val expectedCategory:String,val actualCategory:String):ValidationRequest
    data class StructureFunction(val expectedPairs:Map<String,String>,val actualPairs:Map<String,String>):ValidationRequest
    data class GeneticsRatio(val expected:List<Int>,val actual:String):ValidationRequest
}
interface AnswerValidationEngine { fun validate(request:ValidationRequest):ValidationResult }

class LocalAnswerValidationEngine:AnswerValidationEngine{
 override fun validate(request:ValidationRequest):ValidationResult=when(request){
  is ValidationRequest.Numeric->{val n=request.actual.trim().toDoubleOrNull();result(n!=null&&abs(n-request.expected)<=request.tolerance,if(n==null)LearnerErrorType.ARITHMETIC else null,"numeric")}
  is ValidationRequest.Fraction->{val p=parseRatio(request.actual);val ok=p!=null&&request.expectedNumerator*p.second==p.first*request.expectedDenominator;result(ok,LearnerErrorType.ARITHMETIC,"fraction_equivalence")}
  is ValidationRequest.Algebraic->{val samples=listOf(-3.0,-1.0,.5,2.0,5.0);val ok=samples.all{x->val a=evaluate(request.expected,request.variable,x);val b=evaluate(request.actual,request.variable,x);a!=null&&b!=null&&abs(a-b)<1e-6};result(ok,LearnerErrorType.ALGEBRAIC_TRANSFORMATION,"algebraic_equivalence")}
  is ValidationRequest.OrderedSteps->{val first=request.expected.indices.firstOrNull{i->request.actual.getOrNull(i)?.normal()!=request.expected[i].normal()};ValidationResult(first==null&&request.actual.size==request.expected.size,if(first==null)1.0 else first.toDouble()/request.expected.size,first,if(first==null)null else LearnerErrorType.ALGEBRAIC_TRANSFORMATION,if(first==null)"steps_valid" else "first_invalid_step",emptySet())}
  is ValidationRequest.GraphCharacteristics->{val expected=request.expected.mapKeys{it.key.normal()}.mapValues{it.value.normal()};val actual=request.actual.mapKeys{it.key.normal()}.mapValues{it.value.normal()};val matches=expected.count{actual[it.key]==it.value};ValidationResult(matches==expected.size,matches.toDouble()/expected.size.coerceAtLeast(1),null,if(matches==expected.size)null else LearnerErrorType.GRAPH_INTERPRETATION,"graph_characteristics",emptySet())}
  is ValidationRequest.Physics->{val n=request.actualValue.toDoubleOrNull();when{n==null->result(false,LearnerErrorType.ARITHMETIC,"invalid_number");request.actualUnit.isNullOrBlank()->result(false,LearnerErrorType.UNIT_MISSING,"unit_required");normalUnit(request.actualUnit)!=normalUnit(request.requiredUnit)->result(false,LearnerErrorType.DIMENSION,"unit_mismatch");request.requiredSign!=null&&n.sign()!=request.requiredSign->result(false,LearnerErrorType.SIGN,"sign_convention");request.expectedDirection!=null&&request.actualDirection?.normal()!=request.expectedDirection.normal()->result(false,LearnerErrorType.VECTOR_DIRECTION,"vector_direction");request.requiredSignificantFigures!=null&&significantFigures(request.actualValue)!=request.requiredSignificantFigures->result(false,LearnerErrorType.SIGNIFICANT_FIGURES,"significant_figures");request.plausibleRange!=null&&n !in request.plausibleRange->result(false,LearnerErrorType.RANGE,"physically_implausible");else->result(abs(n-request.expected)<=request.tolerance,if(abs(n-request.expected)<=request.tolerance)null else LearnerErrorType.ARITHMETIC,"physics_numeric")}}
  is ValidationRequest.ChemicalEquation->{val atoms=request.reactants==request.products;val charge=request.reactantCharge==request.productCharge;ValidationResult(atoms&&charge,(if(atoms).7 else 0.0)+(if(charge).3 else 0.0),null,when{!atoms->LearnerErrorType.ATOM_CONSERVATION;!charge->LearnerErrorType.CHARGE_CONSERVATION;else->null},when{!atoms->"atoms_not_conserved";!charge->"charge_not_conserved";else->"equation_balanced"},emptySet())}
  is ValidationRequest.MoleRatio->{result(parseRatioList(request.actual)?.let(::reduce)==reduce(request.expected),LearnerErrorType.FORMULA,"mole_ratio")}
  is ValidationRequest.FormulaWriting->{val atoms=request.expectedElements==request.actualElements;val charge=request.expectedCharge==request.actualCharge;ValidationResult(atoms&&charge,(if(atoms).7 else 0.0)+(if(charge).3 else 0.0),null,when{!atoms->LearnerErrorType.FORMULA;!charge->LearnerErrorType.CHARGE_CONSERVATION;else->null},"formula_writing",emptySet())}
  is ValidationRequest.PH->{val n=request.actual.toDoubleOrNull();when{n==null->result(false,LearnerErrorType.ARITHMETIC,"ph_number");n !in 0.0..14.0->result(false,LearnerErrorType.RANGE,"ph_range");else->result(abs(n-request.expected)<=request.tolerance,LearnerErrorType.ARITHMETIC,"ph")}}
  is ValidationRequest.Labels->{val expected=request.expected.map{it.normal()}.toSet();val actual=request.actual.map{it.normal()}.toSet();ValidationResult(actual.containsAll(expected),actual.intersect(expected).size.toDouble()/expected.size.coerceAtLeast(1),null,if(actual.containsAll(expected))null else LearnerErrorType.LABEL,"label_match",emptySet())}
  is ValidationRequest.Sequence->{val first=request.expected.indices.firstOrNull{i->request.actual.getOrNull(i)?.normal()!=request.expected[i].normal()};ValidationResult(first==null&&request.actual.size==request.expected.size,if(first==null)1.0 else first.toDouble()/request.expected.size,first,if(first==null)null else LearnerErrorType.SEQUENCE,"sequence_order",emptySet())}
  is ValidationRequest.Classification->{result(request.expectedCategory.normal()==request.actualCategory.normal(),LearnerErrorType.CLASSIFICATION,"classification")}
  is ValidationRequest.StructureFunction->{val e=request.expectedPairs.mapKeys{it.key.normal()}.mapValues{it.value.normal()};val a=request.actualPairs.mapKeys{it.key.normal()}.mapValues{it.value.normal()};val n=e.count{a[it.key]==it.value};ValidationResult(n==e.size,n.toDouble()/e.size.coerceAtLeast(1),null,if(n==e.size)null else LearnerErrorType.CLASSIFICATION,"structure_function",emptySet())}
  is ValidationRequest.GeneticsRatio->{val p=parseRatioList(request.actual);val e=reduce(request.expected);val a=p?.let(::reduce);result(a==e,LearnerErrorType.PROBABILITY,"genetics_ratio")}
 }
 private fun result(ok:Boolean,error:LearnerErrorType?,code:String)=ValidationResult(ok,if(ok)1.0 else 0.0,null,if(ok)null else error,if(ok)"${code}_valid" else "${code}_invalid",emptySet())
 private fun String.normal()=lowercase().replace(Regex("\\s+"),"").replace("−","-")
 private fun normalUnit(s:String)=s.lowercase().replace(" ","").replace("²","2").replace("^","")
 private fun parseRatio(raw:String):Pair<Int,Int>?{val p=raw.trim().split('/' , ':');if(p.size!=2)return null;val a=p[0].trim().toIntOrNull()?:return null;val b=p[1].trim().toIntOrNull()?:return null;return if(b==0)null else a to b}
 private fun parseRatioList(raw:String):List<Int>?{val values=mutableListOf<Int>();for(part in raw.trim().split(':','/'))values+=part.trim().toIntOrNull()?:return null;return values.takeIf{it.size>=2}}
 private fun reduce(values:List<Int>):List<Int>{val g=values.map{kotlin.math.abs(it)}.filter{it>0}.reduceOrNull(::gcd)?:1;return values.map{it/g}}
 private fun gcd(a:Int,b:Int):Int=if(b==0)a else gcd(b,a%b)
 private fun Double.sign()=when{this>0->1;this<0->-1;else->0}
 private fun significantFigures(raw:String):Int=raw.trim().lowercase().substringBefore('e').filter{it.isDigit()}.dropWhile{it=='0'}.length.coerceAtLeast(1)
 /** Deliberately small local evaluator for constants, x, +, -, *, / and integer powers. */
 private fun evaluate(raw:String,variable:String,x:Double):Double?=runCatching{Parser(raw.replace(" ",""),variable,x).parse()}.getOrNull()
 private class Parser(private val s:String,private val variable:String,private val x:Double){var i=0
  fun parse():Double=expression().also{require(i==s.length)}
  private fun expression():Double{var v=term();while(i<s.length&&s[i] in "+-"){val op=s[i++];val r=term();v=if(op=='+')v+r else v-r};return v}
  private fun term():Double{var v=factor();while(i<s.length&&s[i] in "*/"){val op=s[i++];val r=factor();v=if(op=='*')v*r else v/r};return v}
  private fun factor():Double{var sign=1.0;if(i<s.length&&s[i]=='-'){sign=-1.0;i++};var v=when{match(variable)->x;i<s.length&&s[i]=='('->{i++;val z=expression();require(i<s.length&&s[i++]==')');z};else->number()};if(i<s.length&&s[i]=='^'){i++;v=v.pow(number())};return sign*v}
  private fun match(token:String)=s.startsWith(token,i).also{if(it)i+=token.length}
  private fun number():Double{val start=i;while(i<s.length&&(s[i].isDigit()||s[i]=='.'))i++;require(i>start);return s.substring(start,i).toDouble()}
 }
}
