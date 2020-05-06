package com.yundao.ydwms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.yundao.ydwms.common.avoidonresult.AvoidOnResult;
import com.yundao.ydwms.protocal.ProductionLogDto;
import com.yundao.ydwms.protocal.request.WareHouseChangingRequest;
import com.yundao.ydwms.protocal.respone.BaseRespone;
import com.yundao.ydwms.protocal.respone.ProductQueryRespone;
import com.yundao.ydwms.protocal.respone.User;
import com.yundao.ydwms.protocal.respone.WarehouseQueryRespone;
import com.yundao.ydwms.retrofit.BaseCallBack;
import com.yundao.ydwms.retrofit.HttpConnectManager;
import com.yundao.ydwms.retrofit.PostRequestService;
import com.yundao.ydwms.util.DialogUtil;
import com.yundao.ydwms.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ProductWarehouseChangingActivity extends ScanProductBaseActivity {

    public EditText barCode ; //条码
    public TextView warehouseName ; //仓库名
    public EditText warehouseNameValue ; //仓库名
    public TextView warehousePositon ; //仓位
    public EditText warehousePositonValue ; //仓位

    private boolean isInit;

    @Override
    protected int getLayout() {
        return R.layout.activity_change_warehouse;
    }

    @Override
    public void dealwithBarcode(String barcodeStr) {
        if( barcodeStr.endsWith( "3") ){//3结尾是仓库的码
            queryWarehouseLog( getActivity(), true, barcodeStr );
        }else {
            productionLog(getActivity(), true, barcodeStr);
        }
    }

    @Override
    public boolean barcodeHasSpecialCondition() {
//        if( foucusEditText == null && productInfos.size() > 0 ){
//            ToastUtil.showShortToast( "一次只能操作一个仓位变更" );
//            return true ;
//        }
        return false;
    }

    @Override
    public void initView(Bundle var1) {
        super.initView(var1);
        barCode = findViewById( R.id.bar_code_value ) ; //条码
        warehouseName = findViewById( R.id.warehouse_name ); //仓库名
        warehouseNameValue = findViewById( R.id.warehouse_name_value ) ; //仓库名
        warehousePositon = findViewById( R.id.warehouse_position); //仓位
        warehousePositonValue = findViewById( R.id.warehouse_position_value ) ; //仓位

        warehouseName.setText( "旧仓位");
        warehouseName.setEnabled( true );
        warehousePositon.setText( "新仓位" );
        warehousePositon.setEnabled( true );
        submit.setOnClickListener( v->{
            if( productInfos.size() == 0 ){
                ToastUtil.showShortToast( "请先扫条形码" );
                return ;
            }
            DialogUtil.showDeclareDialog( getActivity(),  "确定是否上传记录", v1 -> {
                changeWarehousePositon( getActivity(), true, String.valueOf( warehousePositonValue.getTag() ) );
            }).show();
        });

    }

    @Override
    protected void setProductionLogDto(ProductionLogDto productInfo) {
        barCode.setText( productInfo.barCode );
        warehouseNameValue.setText( productInfo.warehouseName + productInfo.warehousePositionCode );
    }

    @Override
    protected void clearProductionLogDto() {
        barCode.setText( "" );
        warehouseNameValue.setText( "" );
    }

    /**
     * 产品信息
     * @param activity
     * @param showProgressDialog
     * @param code
     */
    public void productionLog(Activity activity, boolean showProgressDialog, String code){

        HttpConnectManager manager = new HttpConnectManager.HttpConnectBuilder()
                .setShowProgress(showProgressDialog)
                .build(activity);

        PostRequestService postRequestInterface = manager.createServiceClass(PostRequestService.class);
        Call<ProductQueryRespone> productQueryResponeCall = postRequestInterface.productionLog( code );
        productQueryResponeCall
                .enqueue(new BaseCallBack<ProductQueryRespone>(activity, manager) {
                    @Override
                    public void onResponse(Call<ProductQueryRespone> call, Response<ProductQueryRespone> response) {
                        super.onResponse(call, response);
                        ProductQueryRespone body = response.body();
                        if( body != null && response.code() == 200 ){
//                            int totalElements = body.totalElements;
                            ProductionLogDto[] content = body.content;
                            if(/* totalElements == content.length && */content.length > 0 ){
                                for( int i = 0 ; i < content.length ; i ++ ){
                                    ProductionLogDto info = content[i];
                                    if( info.state == 1 ){ //产品打包，如果是已打包
                                        ToastUtil.showShortToast( "该产品已打包");
                                        barCode.setText( "" );
                                        continue;
                                    }
                                    deleteOperators.add( "delete" );
                                    productInfos.add( info );
                                    if (!isInit) {
                                        pl_root.setAdapter(adapter);
                                        isInit = true;
                                    } else {
                                        adapter.notifyDataSetChanged();
                                    }
                                    totalCount.setText("合计：" + productInfos.size() + "件");
                                    setProductionLogDto( info );

                                }
                            }else{
                                ToastUtil.showShortToast( "不能识别该产品" );
                            }
                        }else if( response.code() == 400 ){
                            ToastUtil.showShortToast( "不能识别该产品" );
                        }else if( response.code() == 401 ){
                            ToastUtil.showShortToast( "登录过期，请重新登录" );
                            AvoidOnResult avoidOnResult = new AvoidOnResult( getActivity() );
                            Intent intent = new Intent( getActivity(), LoginActivity.class );
                            avoidOnResult.startForResult(intent, new AvoidOnResult.Callback() {
                                @Override
                                public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                    if( resultCode == Activity.RESULT_OK ){
                                        User user = YDWMSApplication.getInstance().getUser();
                                        if( user != null ){
                                            operator.setText( "操作员：" + user.username );
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
    }
    /**
     * 查仓位号信息
     * @param activity
     * @param showProgressDialog
     * @param code
     */
    public void queryWarehouseLog(Activity activity, boolean showProgressDialog, String code){

        HttpConnectManager manager = new HttpConnectManager.HttpConnectBuilder()
                .setShowProgress(showProgressDialog)
                .build(activity);

        PostRequestService postRequestInterface = manager.createServiceClass(PostRequestService.class);
        Call<WarehouseQueryRespone> productQueryResponeCall = postRequestInterface.queryWarehouselog( code );
        productQueryResponeCall
                .enqueue(new BaseCallBack<WarehouseQueryRespone>(activity, manager) {
                    @Override
                    public void onResponse(Call<WarehouseQueryRespone> call, Response<WarehouseQueryRespone> response) {
                        super.onResponse(call, response);
                        WarehouseQueryRespone body = response.body();
                        if( body != null && response.code() == 200 ){
//                            int totalElements = body.totalElements;
                            WarehouseQueryRespone.WarehouseLog[] content = body.content;
                            if(/* totalElements == content.length && */content.length > 0 ){
                                for( int i = 0 ; i < content.length ; i ++ ){
                                    WarehouseQueryRespone.WarehouseLog info = content[i];
                                    warehousePositonValue.setText( info.warehouseName + info.code );
                                    warehousePositonValue.setTag( info.code );
                                }
                            }else{
                                ToastUtil.showShortToast( "不能识别该产品" );
                            }
                        }else if( response.code() == 400 ){
                            ToastUtil.showShortToast( "不能识别该产品" );
                        }else if( response.code() == 401 ){
                            ToastUtil.showShortToast( "登录过期，请重新登录" );
                            AvoidOnResult avoidOnResult = new AvoidOnResult( getActivity() );
                            Intent intent = new Intent( getActivity(), LoginActivity.class );
                            avoidOnResult.startForResult(intent, new AvoidOnResult.Callback() {
                                @Override
                                public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                    if( resultCode == Activity.RESULT_OK ){
                                        User user = YDWMSApplication.getInstance().getUser();
                                        if( user != null ){
                                            operator.setText( "操作员：" + user.username );
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
    }

    /**
     * 仓位变更
     * @param activity
     * @param showProgressDialog
     * @param wearhouseCode
     */
    public void changeWarehousePositon(Activity activity, boolean showProgressDialog, String wearhouseCode ){

        WareHouseChangingRequest vo = new WareHouseChangingRequest();
//        vo.codes = list ;
        List<Long> longList = new ArrayList<>();
        for( int i = 0 ; i < productInfos.size() ; i ++ ){
            longList.add( productInfos.get(i).id );
        }
        vo.ids = longList ;
        vo.warehousePositionCode = wearhouseCode;

        HttpConnectManager manager = new HttpConnectManager.HttpConnectBuilder()
                .setShowProgress(showProgressDialog)
                .build(activity);

        PostRequestService postRequestInterface = manager.createServiceClass(PostRequestService.class);
        postRequestInterface.changeWarehousePositon( vo )
                .enqueue(new BaseCallBack<BaseRespone>(activity, manager) {
                    @Override
                    public void onResponse(Call<BaseRespone> call, Response<BaseRespone> response) {
                        super.onResponse(call, response);
                        if( response.code() == 200 || response.code() == 204 ){
                            ToastUtil.showShortToast( "仓位变更成功" );
//                            productInfos.clear();
//                            deleteOperators.clear();
//                            clearProductionLogDto();
//                            totalCount.setText("");
//                            adapter.notifyDataSetChanged();
                            Intent intent = new Intent(getActivity(), UploadSuccessActivity.class);
                            startActivity( intent );
                        }else if( response.code() == 401 ){
                            ToastUtil.showShortToast( "登录过期，请重新登录" );
                            AvoidOnResult avoidOnResult = new AvoidOnResult( getActivity() );
                            Intent intent = new Intent( getActivity(), LoginActivity.class );
                            avoidOnResult.startForResult(intent, new AvoidOnResult.Callback() {
                                @Override
                                public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                    if( resultCode == Activity.RESULT_OK ){
                                        User user = YDWMSApplication.getInstance().getUser();
                                        if( user != null ){
                                            operator.setText( "操作员：" + user.username );
                                        }
                                    }
                                }
                            });
                        }
                    }

                });

    }
}
