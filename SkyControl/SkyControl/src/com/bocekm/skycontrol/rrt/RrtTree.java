/*
 * Copyright (c) 2014, Michal Bocek, All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bocekm.skycontrol.rrt;

import java.util.Iterator;

import com.savarese.spatial.GenericPoint;
import com.savarese.spatial.KDTree;
import com.savarese.spatial.NearestNeighbors;
import com.savarese.spatial.NearestNeighbors.Entry;
import com.vividsolutions.jts.geom.Point;

/**
 * Represents two-dimensional K-D tree. Sets orthodromic distance function.
 */
public class RrtTree implements
        Iterable<RrtNode> {

    private KDTree<Double, GenericPoint<Double>, RrtNode> theTree;
    private RrtNode root;
    private NearestNeighbors<Double, GenericPoint<Double>, RrtNode> neighborQuery;

    public RrtTree(RrtNode root) {
        this.root = root;
        theTree = new KDTree<Double, GenericPoint<Double>, RrtNode>(2);
        add(root);
        OrthodromicDistance<Double, GenericPoint<Double>> orthodromDistance = new OrthodromicDistance<Double, GenericPoint<Double>>();
        neighborQuery = new NearestNeighbors<Double, GenericPoint<Double>, RrtNode>(orthodromDistance);
    }

    public void add(RrtNode n) {
        Point pt = n.getPoint();
        theTree.put(new GenericPoint<Double>(pt.getX(), pt.getY()), n);
    }

    public RrtNode closestTo(Point pt) {
        // see if the actual point is there
        GenericPoint<Double> ptRep = new GenericPoint<Double>(pt.getX(), pt.getY());
        if (theTree.containsKey(ptRep))
            return theTree.get(ptRep);
        Entry<Double, GenericPoint<Double>, RrtNode>[] neighbors =
                neighborQuery.get(theTree, ptRep, 1);
        if (neighbors.length > 0) {
            return neighbors[0].getNeighbor().getValue();
        } else
            return null;
    }

    public RrtNode getRoot() {
        return root;
    }

    public int getNodeCount() {
        return theTree.size();
    }

    @Override
    public Iterator<RrtNode> iterator() {
        return theTree.values().iterator();
    }
}
