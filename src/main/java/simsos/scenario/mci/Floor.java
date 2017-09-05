package simsos.scenario.mci;

import java.util.ArrayList;

/**
 *
 * Created by Youlim Jung on 05/09/2017.
 *
 */
public class Floor {
    private ArrayList<Integer>[][] floorMap;

    public Floor(int radius) {
        floorMap = new ArrayList[radius+1][radius+1];
    }

    private void initMap(){
        for(int i=0; i<floorMap.length; i++)
            for(int j=0 ; j<floorMap.length; j++)
                floorMap[i][j] = new ArrayList<>();
    }

    public ArrayList<Integer>[][] getFloorMap() {
        return floorMap;
    }

    public void setFloorMap(ArrayList<Integer>[][] floorMap) {
        this.floorMap = floorMap;
    }

    public void setPatientOnFloor(int idx, int x, int y){
        this.floorMap[x][y].add(idx);
    }
}
