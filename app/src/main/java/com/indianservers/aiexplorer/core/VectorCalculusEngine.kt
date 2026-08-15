package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class MatrixAnalysisResult(val values: List<List<Double>>, val variables: List<String>, val point: Map<String, Double>, val symmetryError: Double = 0.0, val verification: String)
data class ConstrainedCriticalPoint(val point: Vec2, val multiplier: Double, val value: Double, val constraintResidual: Double, val stationarityResidual: Double)
data class IntegralCertificate(val value: Double, val errorEstimate: Double, val method: String, val orientation: String, val steps: List<String>, val verification: String)
data class TheoremCertificate(val theorem: String, val boundaryValue: Double, val interiorValue: Double, val residual: Double, val passed: Boolean, val assumptions: List<String>, val steps: List<String>)

/** Numerical multivariable/vector calculus with independent identity checks and explicit orientation. */
class VectorCalculusEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun jacobian(sources: List<String>, variables: List<String>, point: Map<String, Double>): MatrixAnalysisResult {
        require(sources.isNotEmpty() && variables.isNotEmpty() && variables.all(point::containsKey))
        val compiled = sources.map(expressions::compile)
        val rows = compiled.map { function -> variables.map { variable -> partial(function, variable, point) } }
        return MatrixAnalysisResult(rows, variables, point, verification = "Each entry was independently evaluated by a centred partial difference with scale-aware step size.")
    }

    fun hessian(source: String, variables: List<String>, point: Map<String, Double>): MatrixAnalysisResult {
        require(variables.isNotEmpty() && variables.all(point::containsKey)); val f = expressions.compile(source)
        val rows = variables.map { first -> variables.map { second -> secondPartial(f, first, second, point) } }
        val symmetry = rows.indices.maxOfOrNull { i -> rows.indices.maxOf { j -> abs(rows[i][j] - rows[j][i]) } } ?: 0.0
        return MatrixAnalysisResult(rows, variables, point, symmetry, "Mixed-partial symmetry residual is ${scientific(symmetry)}.")
    }

    fun lagrange2D(objective: String, constraint: String, level: Double, seeds: List<Vec2>): List<ConstrainedCriticalPoint> {
        require(seeds.isNotEmpty()); val f = expressions.compile(objective); val g = expressions.compile(constraint)
        return seeds.mapNotNull { seed ->
            var state = doubleArrayOf(seed.x, seed.y, 0.0); var converged = false
            repeat(40) {
                val residual = lagrangeResidual(f, g, level, state)
                if (norm(residual) < 1e-9) { converged = true; return@repeat }
                val jacobian = numericalJacobian3({ value -> lagrangeResidual(f, g, level, value) }, state)
                val delta = solve3(jacobian, residual.map { -it }.toDoubleArray()) ?: return@mapNotNull null
                state = DoubleArray(3) { state[it] + delta[it] }
                if (norm(delta) < 1e-10) converged = true
            }
            if (!converged) return@mapNotNull null
            val p = mapOf("x" to state[0], "y" to state[1]); val residual = lagrangeResidual(f, g, level, state)
            ConstrainedCriticalPoint(Vec2(state[0], state[1]), state[2], f.eval(p), abs(g.eval(p) - level), sqrt(residual[0] * residual[0] + residual[1] * residual[1]))
        }.distinctBy { "%.6f,%.6f".format(it.point.x, it.point.y) }.sortedBy { it.value }
    }

    fun scalarLineIntegral(source: String, x: String, y: String, z: String = "0", from: Double, to: Double): IntegralCertificate {
        val scalar = expressions.compile(source); val coordinates = listOf(x, y, z).map(expressions::compile); var evaluations = 0
        fun integrand(t: Double): Double { evaluations++; val p = coordinates.map { it.eval(mapOf("t" to t)) }; val derivative = coordinates.map { derivative(it, "t", mapOf("t" to t)) }; return scalar.eval(mapOf("x" to p[0], "y" to p[1], "z" to p[2])) * sqrt(derivative.sumOf { it * it }) }
        return certified(from, to, ::integrand, "scalar line integral", "increasing t", listOf("Parameterize C by r(t).", "Compute |r'(t)|.", "Integrate f(r(t))*|r'(t)|."), evaluationsProvider = { evaluations })
    }

    fun workLineIntegral(field: List<String>, x: String, y: String, z: String = "0", from: Double, to: Double): IntegralCertificate {
        require(field.size in 2..3); val vector = (field + List(3 - field.size) { "0" }).map(expressions::compile); val coordinates = listOf(x, y, z).map(expressions::compile); var evaluations = 0
        fun integrand(t: Double): Double { evaluations++; val p = coordinates.map { it.eval(mapOf("t" to t)) }; val d = coordinates.map { derivative(it, "t", mapOf("t" to t)) }; val values = vector.map { it.eval(mapOf("x" to p[0], "y" to p[1], "z" to p[2])) }; return values.indices.sumOf { values[it] * d[it] } }
        return certified(from, to, ::integrand, "vector work integral", "increasing t", listOf("Parameterize the oriented curve.", "Substitute r(t) into F.", "Integrate F(r(t)) dot r'(t)."), evaluationsProvider = { evaluations })
    }

    fun surfaceFlux(field: List<String>, surface: List<String>, u: ClosedFloatingPointRange<Double>, v: ClosedFloatingPointRange<Double>): IntegralCertificate {
        require(field.size == 3 && surface.size == 3); val vector = field.map(expressions::compile); val r = surface.map(expressions::compile); var evaluations = 0
        fun outer(a: Double): Double = adaptive(v.start, v.endInclusive, 1e-7) { b ->
            evaluations++; val vars = mapOf("u" to a, "v" to b); val p = r.map { it.eval(vars) }
            val ru = r.map { derivative(it, "u", vars) }; val rv = r.map { derivative(it, "v", vars) }
            val normal = cross(Vec3(ru[0], ru[1], ru[2]), Vec3(rv[0], rv[1], rv[2])); val f = vector.map { it.eval(mapOf("x" to p[0], "y" to p[1], "z" to p[2])) }
            f[0] * normal.x + f[1] * normal.y + f[2] * normal.z
        }
        val value = adaptive(u.start, u.endInclusive, 1e-7, ::outer)
        return IntegralCertificate(value, 1e-6, "parametric surface flux", "r_u cross r_v", listOf("Compute r_u and r_v.", "Choose normal r_u cross r_v.", "Integrate F(r(u,v)) dot normal."), "$evaluations oriented surface samples; reversing parameter order negates the result.")
    }

    fun greenRectangle(p: String, q: String, x: ClosedFloatingPointRange<Double>, y: ClosedFloatingPointRange<Double>): TheoremCertificate {
        val boundary = workLineIntegral(listOf(p, q), "t", y.start.toString(), from = x.start, to = x.endInclusive).value +
            workLineIntegral(listOf(p, q), x.endInclusive.toString(), "t", from = y.start, to = y.endInclusive).value +
            workLineIntegral(listOf(p, q), "t", y.endInclusive.toString(), from = x.endInclusive, to = x.start).value +
            workLineIntegral(listOf(p, q), x.start.toString(), "t", from = y.endInclusive, to = y.start).value
        val pf = expressions.compile(p); val qf = expressions.compile(q)
        val interior = adaptive(x.start, x.endInclusive, 1e-7) { xv -> adaptive(y.start, y.endInclusive, 1e-7) { yv -> partial(qf, "x", mapOf("x" to xv, "y" to yv)) - partial(pf, "y", mapOf("x" to xv, "y" to yv)) } }
        return theorem("Green", boundary, interior, listOf("P,Q have continuous partial derivatives", "counter-clockwise rectangular boundary"), listOf("Evaluate closed-boundary work.", "Integrate dQ/dx-dP/dy over the rectangle."))
    }

    fun gaussBox(field: List<String>, x: ClosedFloatingPointRange<Double>, y: ClosedFloatingPointRange<Double>, z: ClosedFloatingPointRange<Double>): TheoremCertificate {
        require(field.size == 3); val f = field.map(expressions::compile)
        fun face(component: Int, fixed: Double, sign: Double, a: ClosedFloatingPointRange<Double>, b: ClosedFloatingPointRange<Double>, names: Pair<String,String>): Double = adaptive(a.start,a.endInclusive,1e-6){ av -> adaptive(b.start,b.endInclusive,1e-6){ bv -> f[component].eval(mapOf(names.first to av,names.second to bv,listOf("x","y","z")[component] to fixed))*sign } }
        val boundary = face(0,x.endInclusive,1.0,y,z,"y" to "z")+face(0,x.start,-1.0,y,z,"y" to "z")+face(1,y.endInclusive,1.0,x,z,"x" to "z")+face(1,y.start,-1.0,x,z,"x" to "z")+face(2,z.endInclusive,1.0,x,y,"x" to "y")+face(2,z.start,-1.0,x,y,"x" to "y")
        val interior = adaptive(x.start,x.endInclusive,1e-6){ xv -> adaptive(y.start,y.endInclusive,1e-6){ yv -> adaptive(z.start,z.endInclusive,1e-6){ zv -> val point=mapOf("x" to xv,"y" to yv,"z" to zv); partial(f[0],"x",point)+partial(f[1],"y",point)+partial(f[2],"z",point) } } }
        return theorem("Gauss divergence",boundary,interior,listOf("F is continuously differentiable","outward box normals"),listOf("Sum flux over six faces.","Integrate div(F) over the volume."))
    }

    fun stokesPlanarRectangle(field: List<String>, x: ClosedFloatingPointRange<Double>, y: ClosedFloatingPointRange<Double>, z: Double = 0.0): TheoremCertificate {
        require(field.size == 3); val boundary = workLineIntegral(field,"t",y.start.toString(),z.toString(),x.start,x.endInclusive).value+workLineIntegral(field,x.endInclusive.toString(),"t",z.toString(),y.start,y.endInclusive).value+workLineIntegral(field,"t",y.endInclusive.toString(),z.toString(),x.endInclusive,x.start).value+workLineIntegral(field,x.start.toString(),"t",z.toString(),y.endInclusive,y.start).value
        val fx=expressions.compile(field[0]); val fy=expressions.compile(field[1]); val interior=adaptive(x.start,x.endInclusive,1e-7){ xv -> adaptive(y.start,y.endInclusive,1e-7){ yv -> val point=mapOf("x" to xv,"y" to yv,"z" to z); partial(fy,"x",point)-partial(fx,"y",point) } }
        return theorem("Stokes",boundary,interior,listOf("F is continuously differentiable","upward normal and counter-clockwise boundary"),listOf("Evaluate boundary circulation.","Integrate curl(F) dot k over the planar surface."))
    }

    private fun theorem(name:String,boundary:Double,interior:Double,assumptions:List<String>,steps:List<String>):TheoremCertificate { val residual=abs(boundary-interior); return TheoremCertificate(name,boundary,interior,residual,residual<1e-5*max(1.0,max(abs(boundary),abs(interior))),assumptions,steps) }
    private fun certified(from:Double,to:Double,f:(Double)->Double,method:String,orientation:String,steps:List<String>,evaluationsProvider:()->Int):IntegralCertificate { val fine=adaptive(from,to,1e-8,f); val coarse=adaptive(from,to,1e-6,f); return IntegralCertificate(fine,abs(fine-coarse),method,orientation,steps,"${evaluationsProvider()} evaluations; fine/coarse difference ${scientific(abs(fine-coarse))}.") }
    private fun partial(f:Expression, variable:String, point:Map<String,Double>):Double { val center=point.getValue(variable); val h=1e-5*max(1.0,abs(center)); return (f.eval(point+(variable to center+h))-f.eval(point+(variable to center-h)))/(2*h) }
    private fun derivative(f:Expression, variable:String, point:Map<String,Double>)=partial(f,variable,point)
    private fun secondPartial(f:Expression,a:String,b:String,p:Map<String,Double>):Double { val center=p.getValue(b); val h=2e-4*max(1.0,abs(center)); return (partial(f,a,p+(b to center+h))-partial(f,a,p+(b to center-h)))/(2*h) }
    private fun lagrangeResidual(f:Expression,g:Expression,level:Double,s:DoubleArray):DoubleArray { val p=mapOf("x" to s[0],"y" to s[1]); return doubleArrayOf(partial(f,"x",p)-s[2]*partial(g,"x",p),partial(f,"y",p)-s[2]*partial(g,"y",p),g.eval(p)-level) }
    private fun numericalJacobian3(fn:(DoubleArray)->DoubleArray,s:DoubleArray):Array<DoubleArray> { val h=1e-5; return Array(3){row->DoubleArray(3){col->val plus=s.copyOf();val minus=s.copyOf();plus[col]+=h;minus[col]-=h;(fn(plus)[row]-fn(minus)[row])/(2*h)}} }
    private fun solve3(a:Array<DoubleArray>,b:DoubleArray):DoubleArray? { val m=Array(3){i->DoubleArray(4){j->if(j<3)a[i][j] else b[i]}}; for(c in 0..2){val pivot=(c..2).maxByOrNull{abs(m[it][c])}!!;if(abs(m[pivot][c])<1e-12)return null;val tmp=m[c];m[c]=m[pivot];m[pivot]=tmp;val d=m[c][c];for(j in c..3)m[c][j]/=d;for(i in 0..2)if(i!=c){val q=m[i][c];for(j in c..3)m[i][j]-=q*m[c][j]}};return DoubleArray(3){m[it][3]} }
    private fun norm(v:DoubleArray)=sqrt(v.sumOf{it*it})
    private fun cross(a:Vec3,b:Vec3)=Vec3(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)
    private fun adaptive(from: Double, to: Double, tolerance: Double, f: (Double) -> Double): Double {
        if (from == to) return 0.0
        fun simpson(a: Double, b: Double, fa: Double, fm: Double, fb: Double) = (b - a) * (fa + 4 * fm + fb) / 6
        fun recurse(a: Double, b: Double, fa: Double, fm: Double, fb: Double, whole: Double, tol: Double, depth: Int): Double {
            val middle = (a + b) / 2; val lm = (a + middle) / 2; val rm = (middle + b) / 2
            val fl = f(lm); val fr = f(rm); val left = simpson(a, middle, fa, fl, fm); val right = simpson(middle, b, fm, fr, fb); val delta = left + right - whole
            return if (depth == 0 || abs(delta) <= 15 * tol) left + right + delta / 15
            else recurse(a, middle, fa, fl, fm, left, tol / 2, depth - 1) + recurse(middle, b, fm, fr, fb, right, tol / 2, depth - 1)
        }
        val middle = (from + to) / 2; val fa = f(from); val fm = f(middle); val fb = f(to)
        return recurse(from, to, fa, fm, fb, simpson(from, to, fa, fm, fb), tolerance, 16)
    }
    private fun scientific(value:Double)=String.format(java.util.Locale.US,"%.3e",value)
}
