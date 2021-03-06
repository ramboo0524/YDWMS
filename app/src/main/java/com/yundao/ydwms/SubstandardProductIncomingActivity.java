package com.yundao.ydwms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.yundao.ydwms.LoginActivity;
import com.yundao.ydwms.ProductCatalogueActivity;
import com.yundao.ydwms.R;
import com.yundao.ydwms.ScanProductBaseActivity;
import com.yundao.ydwms.UploadSuccessActivity;
import com.yundao.ydwms.YDWMSApplication;
import com.yundao.ydwms.common.avoidonresult.AvoidOnResult;
import com.yundao.ydwms.protocal.ProductionLogDto;
import com.yundao.ydwms.protocal.request.WarehouseVo;
import com.yundao.ydwms.protocal.respone.BaseRespone;
import com.yundao.ydwms.protocal.respone.ProductQueryRespone;
import com.yundao.ydwms.protocal.respone.ProductStateEnums;
import com.yundao.ydwms.protocal.respone.User;
import com.yundao.ydwms.protocal.respone.WarehouseRespone;
import com.yundao.ydwms.retrofit.BaseCallBack;
import com.yundao.ydwms.retrofit.HttpConnectManager;
import com.yundao.ydwms.retrofit.PostRequestService;
import com.yundao.ydwms.util.DialogUtil;
import com.yundao.ydwms.util.SharedPreferenceUtil;
import com.yundao.ydwms.util.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class SubstandardProductIncomingActivity extends ScanProductBaseActivity {

    private int index = 0 ;
    private String[] codes = new String[]{ "15909214791831", "15908468698451", "15908482382951", "15908380552481" };

    public String[] warehouseNameStrs = new String[]{ "不良品仓(次品)", "不良品仓(待处理)" } ;
    public EditText barCode ; //条码
    public EditText material ; //料号
    public EditText productName ; //品名
    public TextView warehouseName ; //仓库名
    public EditText warehousePositon ; //仓位

    private boolean isInit;

    @Override
    protected int getLayout() {
        return R.layout.activity_substandard_product_incoming;
    }

    @Override
    public void dealwithBarcode(String barcodeStr) {
        productionLog( getActivity(), true, barcodeStr, ProductStateEnums.INCOMING );
    }

    @Override
    protected void setTitleBar() {
        super.setTitleBar();
        titleBar.setRightText( "分类汇总" )
                .setRightTitleClickListener( v->{
                    if( productInfos.size() > 0 ) {
                        AvoidOnResult onResult = new AvoidOnResult(getActivity());
                        Intent intent = new Intent(getActivity(), ProductCatalogueActivity.class);
                        intent.putExtra("productInfoList", productInfos);
                        onResult.startForResult(intent, new AvoidOnResult.Callback() {
                            @Override
                            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                if (resultCode == Activity.RESULT_OK) {
                                    List<ProductionLogDto> productInfoList = (List<ProductionLogDto>)data.getSerializableExtra("productInfoList");
                                    productInfos.clear();
                                    deleteOperators.clear();
                                    if( productInfoList.size() > 0 ) {
                                        productInfos.addAll(productInfoList);
                                        for (int i = 0; i < productInfos.size(); i++) {
                                            deleteOperators.add("delete");
                                        }
                                    }
                                    totalCount.setText( "合计：" + productInfos.size() + "件" );
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } );
    }

    @Override
    public boolean barcodeHasSpecialCondition() {
        return false;
    }

    @Override
    public void initView(Bundle var1) {
        super.initView(var1);
        SHARE_PREFERENCE_KEY = "SUBSTANDARD_PRODUCT_INCOMING_KEY";
        barCode = findViewById( R.id.bar_code_value ); //条码
        material = findViewById( R.id.material_value ); //料号
        productName = findViewById( R.id.product_name_value ); //品名
        warehouseName = findViewById( R.id.warehouse_name_value ); //仓库名
        warehousePositon = findViewById( R.id.warehouse_position_value ); //仓位


        warehouseName.setOnClickListener( v -> {
            DialogUtil.showTypeDialog(getActivity(), "请选择仓库", warehouseNameStrs, (dialog, type, position) -> {
                warehouseName.setText( type );
                dialog.cancel();
            });
        } );
        submit.setOnClickListener( v->{

            if( YDWMSApplication.getInstance().isUseLocalData() ) {
                if (index < codes.length) {
                    dealwithBarcode(codes[index]);
                    index++;
                    return;
                }
            }

            if( productInfos.size() == 0 ){
                ToastUtil.showShortToast( "请先扫条形码" );
                return ;
            }

            if( "".equals( warehouseName.getText() ) ){
                ToastUtil.showShortToast( "请选择仓库" );
                return ;
            }
            DialogUtil.showDeclareDialog( getActivity(),  "确定是否上传记录", v1 -> {
                productionIncoming(getActivity(), true);
            }).show();
        });

        barCode.setOnClickListener(v -> DialogUtil.showInputDialog(getActivity(), barCode.getText().toString(), (dialog, type, position) -> {
            barCode.setText( type );
            dealwithBarcode( type );
            dialog.dismiss();
        }));
//        dealwithBarcode("15844258895641" );
        loadFromCache(ProductStateEnums.INCOMING);
    }

    @Override
    protected void setProductionLogDto(ProductionLogDto ProductionLogDto) {
        barCode.setText( ProductionLogDto.barCode );
        material.setText( ProductionLogDto.productModel );
        productName.setText( ProductionLogDto.productName );

    }

    @Override
    protected void clearProductionLogDto() {
        barCode.setText( "" );
        material.setText( "" );
        productName.setText( "" );
    }

    /**
     * 产品进仓
     * @param activity
     * @param showProgressDialog
     */
    public void productionIncoming(Activity activity, boolean showProgressDialog ){

        WarehouseVo vo = new WarehouseVo();
        vo.ids = genCodes();
        vo.warehouseName = warehouseName.getText().toString() ;
        vo.warehousePositionCode = warehousePositon.getText().toString() ;

        HttpConnectManager manager = new HttpConnectManager.HttpConnectBuilder()
                .setShowProgress(showProgressDialog)
                .build(activity);

        PostRequestService postRequestInterface = manager.createServiceClass(PostRequestService.class);
        postRequestInterface.productionIncoming( vo )
                .enqueue(new BaseCallBack<BaseRespone>(activity, manager) {
                    @Override
                    public void onResponse(Call<BaseRespone> call, Response<BaseRespone> response) {
                        super.onResponse(call, response);
                        if( response.code() == 200 || response.code() == 204 ){
                            productInfos.clear();
                            adapter.notifyDataSetChanged();
                            SharedPreferenceUtil.remove( SHARE_PREFERENCE_KEY );
                            ToastUtil.showShortToast( "进仓成功" );
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
                        }else{
                            try {
                                ToastUtil.showShortToast( response.errorBody().string() );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                });

    }
}
