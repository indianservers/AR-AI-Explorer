package com.indianservers.aiexplorer.mathworkspace.lineartransform

import org.junit.Assert.*
import org.junit.Test

class MatrixTransformTest {
    @Test fun identityAndScaleHaveExpectedDeterminantRankAndInverse(){
        val i=MatrixMathEngine.identity;assertEquals(1.0,i.determinant,0.0);assertEquals(2,MatrixMathEngine.rank(i));assertEquals(i,i.inverse())
        val m=Matrix2(2.0,0.0,0.0,3.0);assertEquals(6.0,m.determinant,0.0);assertEquals(Vec2(2.0,0.0),m*Vec2(1.0,0.0));assertEquals(Vec2(0.0,3.0),m*Vec2(0.0,1.0));assertEquals(6.0,DeterminantEngine.areaScale(m),0.0)
    }
    @Test fun singularRanksOrientationAndInverseAvailability(){
        val rankOne=Matrix2(1.0,2.0,2.0,4.0);val zero=Matrix2(0.0,0.0,0.0,0.0)
        assertEquals(1,MatrixMathEngine.rank(rankOne));assertNull(rankOne.inverse());assertEquals(Orientation.Collapsed,DeterminantEngine.orientation(rankOne));assertEquals(0,MatrixMathEngine.rank(zero));assertNull(zero.inverse())
        assertEquals(Orientation.Reversed,DeterminantEngine.orientation(Matrix2(-1.0,0.0,0.0,1.0)))
    }
    @Test fun shearHasRepeatedDefectiveEigenvalueButIdentityHasPlaneOfEigenvectors(){
        val shear=EigenEngine.compute(Matrix2(1.0,1.0,0.0,1.0));assertTrue(shear.real);assertEquals(listOf(1.0),shear.eigenvalues);assertEquals(listOf(1),shear.eigenspaceDimensions)
        val identity=EigenEngine.compute(MatrixMathEngine.identity);assertEquals(listOf(1.0),identity.eigenvalues);assertEquals(listOf(2),identity.eigenspaceDimensions);assertEquals(2,identity.vectors.size)
    }
    @Test fun rotationHasComplexEigenvaluesAndReflectionHasRealOnes(){
        val rotation=EigenEngine.compute(Matrix2(0.0,-1.0,1.0,0.0));assertFalse(rotation.real);assertTrue(rotation.discriminant<0)
        val reflection=EigenEngine.compute(Matrix2(-1.0,0.0,0.0,1.0));assertTrue(reflection.real);assertEquals(setOf(-1.0,1.0),reflection.eigenvalues.toSet())
    }
    @Test fun compositionOrderDiffersAndAnimationReachesEachStage(){
        val a=Matrix2(2.0,0.0,0.0,1.0);val b=Matrix2(1.0,1.0,0.0,1.0);assertNotEquals(a*b,b*a)
        assertEquals(b,TransformAnimationEngine.compositionAt(a,b,.5));assertEquals(a*b,TransformAnimationEngine.compositionAt(a,b,1.0))
    }
    @Test fun angleRotationAndSingularSystemClassification(){
        val r=MatrixMathEngine.rotation(90.0);assertEquals(0.0,r.a,1e-10);assertEquals(-1.0,r.b,1e-10)
        assertEquals(LinearSolveResult.Unique(Vec2(1.0,2.0)),MatrixMathEngine.solve(MatrixMathEngine.identity,Vec2(1.0,2.0)))
        assertEquals(LinearSolveResult.Infinite,MatrixMathEngine.solve(Matrix2(1.0,2.0,2.0,4.0),Vec2(1.0,2.0)))
        assertEquals(LinearSolveResult.None,MatrixMathEngine.solve(Matrix2(1.0,2.0,2.0,4.0),Vec2(1.0,3.0)))
    }
    @Test fun extremeScaleRemainsInvertibleAndFinite(){
        val m=Matrix2(10000.0,0.0,0.0,.0001);assertEquals(2,MatrixMathEngine.rank(m));assertNotNull(m.inverse());assertTrue((m*Vec2(1.0,1.0)).x.isFinite())
    }
    @Test fun draggingBasisColumnsSynchronizesMatrixEntriesAndHonorsLocks(){
        val vm=MatrixTransformViewModel();vm.dragBasis(0,Vec2(2.0,1.0));vm.dragBasis(1,Vec2(-1.0,3.0))
        assertEquals(Matrix2(2.0,-1.0,1.0,3.0),vm.target)
        vm.lockE1.value=true;vm.dragBasis(0,Vec2(8.0,9.0));assertEquals(Matrix2(2.0,-1.0,1.0,3.0),vm.target)
    }
    @Test fun transformedUnitSquareAreaMatchesAbsoluteDeterminant(){
        val m=Matrix2(2.0,0.0,0.0,3.0);val points=ShapeModels.points(ShapeKind.Square).map{m*it};val area=points.zipWithNext().sumOf{(p,q)->p.x*q.y-q.x*p.y}/2
        assertEquals(DeterminantEngine.areaScale(m),area,1e-10)
    }
    @Test fun interpolationCanPassThroughSingularCollapse(){
        val reflection=Matrix2(1.0,0.0,0.0,-1.0);val halfway=TransformAnimationEngine.interpolate(MatrixMathEngine.identity,reflection,.5)
        assertEquals(Orientation.Collapsed,DeterminantEngine.orientation(halfway));assertEquals(1,MatrixMathEngine.rank(halfway))
    }
}
