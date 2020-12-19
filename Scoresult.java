package test1;

import java.util.*;
import java.util.Arrays.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.io.*;

class Purpose {
    String name;
    String value;
}
// 买方信息
class Buyer {
    String Id;
    int Time;
    int Need;
    String Breed;
    Purpose[] Wanted;
    int unsat;
}

// 卖方信息
class Seller {
    String Id;
    String Breed;
    String CargoId;
    int Qty;
    String Store;
    String Brand;
    String Origin;
    String Year;
    String Level;
    String Type;
    int remain;
}
// 分配信息
class Trading {
    String buyer;
    String seller;
    String breed;
    String cargoId;
    String store;
    int dealQty;
    String hopeSat;
    int SCORE;
}

public class Scoresult {
    // 用户名称字符长度
    public static final String buyerDataName = "buyer.csv";
    public static final String sellerDataName = "seller.csv";
    public static final String resfileName = "result19_4.txt";
    public static final int[] scrCF = {33, 27, 20, 13, 7};
    public static final int[] scrSR = {40, 30, 20, 10};
    
    public static void main (String[] args) throws FileNotFoundException, InterruptedException, ExecutionException, IOException {
        // 计时开始
        long startTime = System.currentTimeMillis();
        // 建立线程池
        ExecutorService service = Executors.newCachedThreadPool();
        service.shutdown();

        // 读取交易信息数据
        List<String> tradingData = Files.readAllLines(Paths.get(resfileName), Charset.forName("GBK"));
        int dealsNum = tradingData.size() - 1;
        Trading[] DealInfos = new Trading[dealsNum]; 
        
        int i = 0;
        for (i = 1; i <= dealsNum; i++) {
            String[] info = tradingData.get(i).split(",");
            Trading adeal = new Trading();
            adeal.buyer = info[0];
            adeal.seller = info[1];
            adeal.breed = info[2];
            adeal.cargoId = info[3];
            adeal.store = info[4];
            adeal.dealQty = Integer.parseInt(info[5]);
            adeal.hopeSat = info[6];
            adeal.SCORE = calTradingScore(info[6], info[2]);
            
            DealInfos[i-1] = adeal;
        }

        // 按持仓时间排序，以自然确定优先级
        Arrays.sort(DealInfos, dcomp);

        double CFscore = 0.0;
        double SRscore = 0.0;
        int CFnum = 0;
        int SRnum = 0;
        int hopesc = 0;
        int diarysc = 0;
        double fscr = 0.0;
        double CF_Diarysc = 0;
        double CF_Fscr = 0.0;
        double SR_Diarysc = 0;
        double SR_Fscr = 0.0;
        int onebuyernum = 0;
        int rowStart = 0;
        int storenum = 0;

        String buyi = "";
        for (i = 1; i <= dealsNum; i++) {

            buyi = (i < dealsNum) ? DealInfos[i].buyer : "";

            if (! buyi.equals(DealInfos[i-1].buyer)) {
                onebuyernum = 0;
                hopesc = 0;
                storenum = 0;

                for (int k = rowStart; k < i; k++) {
                    hopesc += DealInfos[k].SCORE * DealInfos[k].dealQty;
                    onebuyernum += DealInfos[k].dealQty;
                    
                    if (k > rowStart) {
                        if (! DealInfos[k].store.equals(DealInfos[k-1].store)) {
                            storenum++;
                        }
                    }
                }

                fscr = (double)hopesc/onebuyernum;
                if (DealInfos[rowStart].breed.equals("CF")) {
                    diarysc = 100 - 100/5*storenum;
                    CFscore += (fscr*0.6 + diarysc*0.4) * onebuyernum;
                    CF_Diarysc += diarysc*0.4* onebuyernum;
                    CF_Fscr += fscr*0.6* onebuyernum;                
                    
                    CFnum += onebuyernum;
                } else {
                    diarysc = 100 - 100/4*storenum;
                    SRscore += (fscr*0.6 + diarysc*0.4) * onebuyernum;
                    SR_Diarysc += diarysc*0.4* onebuyernum;
                    SR_Fscr += fscr*0.6* onebuyernum;  
                    SRnum += onebuyernum;
                }

                rowStart = i;
            } 
        }

        CFscore /= CFnum;
        SRscore /= SRnum;
        
        CF_Diarysc /= CFnum;
        CF_Fscr /= CFnum;
        SR_Diarysc /= SRnum;
        SR_Fscr /= SRnum;

        System.out.println("CFscore:" + CFscore + "\n" + "HopeScore:" + CF_Fscr + "\n" + "DiaryScore:" + CF_Diarysc);
        System.out.println("SRscore:" + SRscore + "\n" + "HopeScore:" + SR_Fscr + "\n" + "DiaryScore:" + SR_Diarysc);
        System.out.println("总分为" + (CFscore+SRscore));

        System.out.println("ALL DONE. Elapse " + (System.currentTimeMillis()-startTime) + "ms");

    }

    static void putInWishMap(String info, HashMap<String, Integer> wishPool) {
        int contVal = 0;
        if ( wishPool.containsKey(info) ) {
            contVal = wishPool.get(info);
        } 

        contVal++;
        wishPool.put(info, contVal);
    }


    static int calBias(Buyer buyi, Seller selli) {
        
        if (buyi.Breed.equals(selli.Breed)) {
            return 100000 - Math.abs(buyi.unsat - selli.remain);
        } else {
            return 0;
        }
        
    }
    
    static int calTradingScore(String satList, String breed) {
        byte[] satbt = satList.getBytes();
        if (satbt[0] == '0') {
            return 0;
        }

        int scr = 0;
        if (breed.equals("SR")) {
            for (int i = 0; i < satbt.length; i += 2) {
                scr += scrSR[satbt[i]-'1'];
            }
        } else {
            for (int i = 0; i < satbt.length; i += 2) {
                scr += scrCF[satbt[i]-'1'];
            }
        }
        
        return scr;
    }
    
    // 计算买卖双方匹配得分
    static int calScore(Buyer buyi, Seller selli) {
        int score = 0;
        if (selli.remain == 0) {
            return 0;
        }

        int[] scrCF = {33, 27, 20, 13, 7};
        int[] scrSR = {40, 30, 20, 10};
        int[] scr = null;
        if (buyi.Breed.equals("CF")) {
            scr = scrCF;
        } else {
            scr = scrSR;
        }

        boolean[] sats = new boolean[5];

        for (int i = 0; i < buyi.Wanted.length; i++) {
            switch (buyi.Wanted[i].name) {
                case "仓库":
                    sats[i] = selli.Store.equals(buyi.Wanted[i].value);
                    break;
                case "品牌":
                    sats[i] = selli.Brand.equals(buyi.Wanted[i].value);
                    break;
                case "产地":
                    sats[i] = selli.Origin.equals(buyi.Wanted[i].value);
                    break;
                case "年度":
                    sats[i] = selli.Year.equals(buyi.Wanted[i].value);
                    break;
                case "等级":
                    sats[i] = selli.Level.equals(buyi.Wanted[i].value);
                    break;
                case "类别":
                    sats[i] = selli.Type.equals(buyi.Wanted[i].value);
                    break;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (sats[i]) {
                score += scr[i];
            }
        }

        score *= Math.min(selli.remain, buyi.unsat);
        if (selli.remain == buyi.unsat) {
            score += 200;
        }

        return score;
    }

    // 得到匹配项顺序的字符串
    static String outputMatch(Buyer buyi, Seller selli) {

        List<Integer> satt = new ArrayList<Integer>();

        for (int i = 0; i < buyi.Wanted.length; i++) {
            switch (buyi.Wanted[i].name) {
                case "仓库":
                    if (selli.Store.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
                case "品牌":
                    if (selli.Brand.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
                case "产地":
                    if (selli.Origin.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
                case "年度":
                    if (selli.Year.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
                case "等级":
                    if (selli.Level.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
                case "类别":
                    if (selli.Type.equals(buyi.Wanted[i].value)) {
                        satt.add(i);
                    }
                    break;
            }
        }

        if (satt.size() == 0) {
            return "0";
        } else {
            String sout = String.valueOf(satt.get(0)+1);
            for (int i = 1; i < satt.size(); i++) {
                sout += "-" + String.valueOf(satt.get(i)+1);
            }
            return sout;
        }
    }

    static String[] mySplit(String strln) {
        String[] res = new String[10];
        int stloc = 0;
        int num = 0;
        for (int i = 0; i < strln.length(); i++) {
            if (strln.charAt(i) == ',') {
                res[num] = strln.substring(stloc, i);
                num++;
                stloc = i+1;
            }
        }
        return res;
    }

    static List<Integer> routbest(int obj, int[] candidate) {
        List<Integer> elected = new ArrayList<Integer>();
        int len = candidate.length;
        for (int i = 0; i < len; i++) {
            if (candidate[i] == obj) {
                elected.add(i);
                return elected;
            }
        }

        int maxloc = getmaxloc(candidate);
        elected.add(maxloc);
        if (candidate[maxloc] > obj) {
            elected.add(obj);
            return elected;
        }

        int valsum = candidate[maxloc];
        while (valsum < obj) {
            candidate[maxloc] = 0;
            maxloc = getmaxloc(candidate);
            if (candidate[maxloc] == 0) {
                return elected;
            }
            elected.add(maxloc);
            valsum += candidate[maxloc];
        }

        return elected;
    }

    static int getmaxloc(int[] vec) {
        int loc = 0;
        int maxval = 0;
        for (int i = 0; i < vec.length; i++) {
            if (vec[i] >= maxval) {
                maxval = vec[i];
                loc = i;
            }
        }
        return loc;
    }

    static boolean[] expectSat (Buyer buyi, Seller selli, int n) {
        boolean[] sats = new boolean[5];
        for (int i = 0; i < Math.min(buyi.Wanted.length, n); i++) {
            switch (buyi.Wanted[i].name) {
                case "仓库":
                    sats[i] = selli.Store.equals(buyi.Wanted[i].value);
                    break;
                case "品牌":
                    sats[i] = selli.Brand.equals(buyi.Wanted[i].value);
                    break;
                case "产地":
                    sats[i] = selli.Origin.equals(buyi.Wanted[i].value);
                    break;
                case "年度":
                    sats[i] = selli.Year.equals(buyi.Wanted[i].value);
                    break;
                case "等级":
                    sats[i] = selli.Level.equals(buyi.Wanted[i].value);
                    break;
                case "类别":
                    sats[i] = selli.Type.equals(buyi.Wanted[i].value);
                    break;
            }
        }
        return sats;
    } 

    // public static Comparator<Buyer> bcomp = new Comparator<Buyer> (){
    //     public int compare(Buyer b1, Buyer b2){

    //         int cmb = b2.Breed.compareTo(b1.Breed);
    //         if (cmb != 0) {
    //             return cmb;
    //         }
            
    //         int nb1 = b1.Wanted.length;
    //         int nb2 = b2.Wanted.length;
    //         if ((nb1 > 0) && (nb2 > 0)) {
    //             int cmn = b2.Wanted[0].name.compareTo(b1.Wanted[0].name);
    //             if (cmn == 0) {
    //                 int cmv = b2.Wanted[0].value.compareTo(b1.Wanted[0].value);
    //                 if (cmv != 0) {
    //                     return cmv;
    //                 }
    //             } else {
    //                 return cmn;
    //             }
    //         } else if ((nb1 > 0) || (nb2 > 0)) {
    //             return (nb2 - nb1);
    //         }
            
    //         return (b2.Time - b1.Time);
    //     }
    // };

    public static Comparator<Trading> dcomp = new Comparator<Trading> (){
        public int compare(Trading d1, Trading d2){  
            int cmb = d1.breed.compareTo(d2.breed);
            if (cmb != 0) {
                return cmb;
            }

            cmb = d1.buyer.compareTo(d2.buyer);
            if (cmb != 0) {
                return cmb;
            }

            return (d1.store.compareTo(d2.store));
        }
    };

    public static Comparator<Seller> scomp = new Comparator<Seller> (){
        public int compare(Seller s1, Seller s2){
            return (s2.Qty - s1.Qty);
        }
    };

}

