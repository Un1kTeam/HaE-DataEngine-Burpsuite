package burp;

import burp.utils.Functions;
import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.PrintWriter;

import static burp.utils.Functions.*;


public class BurpExtender implements IBurpExtender,IHttpListener{

    public  Connection NC = null;
    public PrintWriter stdout;
    public IExtensionHelpers helpers;
    private IBurpExtenderCallbacks callbacks;
    private burp.classes.Config Config;

    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {

        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("HaE-DataEngine");
        callbacks.registerHttpListener(BurpExtender.this);
        showLog(this.stdout,"info","Load extender successful");
        showLog(this.stdout,"info","Author depy");
        showLog(this.stdout,"info","Version 0.0.4");
        init();

    }


    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {


        Boolean checkExt = Functions.checkExt(helpers.analyzeRequest(messageInfo).getUrl().getPath());

        if(this.Config != null && !checkExt){
            showLog(this.stdout,"info","Non static resources, start regular matching of data, compression and segmentation");
            if(messageIsRequest){
                try {
                    Functions.chunkSend(messageInfo,1,this);
                }catch (Exception e){
                    showLog(this.stdout,"error",e.getMessage());
                }

            }else{
                try {
                    Functions.chunkSend(messageInfo,2,this);
                }catch (Exception e){
                    showLog(this.stdout,"error",e.getMessage());
                }
            }
        }


    }

    public void init(){

        checkAndCreateConfigFile(this,"HaE.ini");

        try{
            this.Config = parseIniFile("HaE.ini");
        }catch (Exception e){
            showLog(this.stdout,"error",e.getMessage());
        }

        try{
            Connection nc = Nats.connect("nats://"+this.Config.getNatsServerHost()+":"+this.Config.getNatsServerPort());
            showLog(this.stdout,"success","Nats Connect Success.");
            this.NC = nc;

        }catch (Exception e){
            this.Config = null;
            showLog(this.stdout,"error",e.getMessage());
        }
    }


}