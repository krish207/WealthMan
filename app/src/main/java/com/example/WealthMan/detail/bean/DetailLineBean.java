
package com.example.WealthMan.detail.bean;

import java.util.List;

public class DetailLineBean {

    /**
     * Copyright 2019 bejson.com
     */

    /**
     * Auto-generated: 2019-02-24 15:31:27
     *
     * @author bejson.com (i@bejson.com)
     * @website http://www.bejson.com/java2pojo/
     */
    public Detaildate AAPL;
    public Detaildate FB;
    public Detaildate AAL;
    public Detaildate BAC;
    public Detaildate WFC;
    public Detaildate WMT;
    public Detaildate COF;
    public Detaildate AMZN;
    public Detaildate VMW;
    public Detaildate IBM;
    public Detaildate DELL;
    public Detaildate HPQ;
    public Detaildate MSFT;
    public Detaildate JNPR;
    public Detaildate ORCL;

    public class Detaildate {
        public DetailNew quote;
        public List<DetailLineDate> chart;

        public class DetailNew {
            public String symbol;
            public String companyName;
            public String news;
            public String open;
            public String latestVolume;
            public float close;
            public float change;
            public float week52High;
            public float week52Low;
            public float ytdChange;
            public float changePercent;


        }

        public class DetailLineDate {
            public float close;
            public String date;
            public String minute;
        }
    }
}
