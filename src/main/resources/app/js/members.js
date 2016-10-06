angular.module('HomeModule')
.controller('MemberLoginCtrl', function($scope, $state, $timeout) {

    $scope.loginMember = function() {
        angular.element("#home-loading-modal").modal('show');
        $timeout(function(){
            var mobileNumber = angular.element('#mobile_no').val();
            if(mobileNumber == '') {
                mobileNumber = 'a';
            }
            $state.go('member-profile-view', {mobileNumber: mobileNumber});
        }, 1000);
    }
})
.controller('MemberProfileCtrl', function($scope, $stateParams, DTOptionsBuilder, DTColumnBuilder, $q, $rootScope) {

    var vm = this;
    $scope.activeVouchers = [];
    var getTableData = function() {
        var deferred = $q.defer();
        deferred.resolve($scope.activeVouchers);
        return deferred.promise;
    };

    vm.dtOptions = DTOptionsBuilder.fromFnPromise(getTableData).withPaginationType('full_numbers');
    vm.dtColumns = [
        DTColumnBuilder.newColumn('name').withTitle('Reward').notSortable(),
        DTColumnBuilder.newColumn('details').withTitle('Details').notSortable(),
        DTColumnBuilder.newColumn('date').withTitle('Redemption Date').notSortable()
    ];
    vm.dtInstance = {};
    vm.dtInstanceCallback = function(_dtInstance) {
        vm.dtInstance = _dtInstance;
        vm.dtInstance.reloadData(); //or something else....
    }
    if ($stateParams.mobileNumber === null) {
        homeService.fetchCustomerData(function(resp) {
            if (resp.message != undefined) {
                $scope.message = 'Customer not found';
            } else {
                $scope.member = {
                    id: resp.data.id,
                    name:  resp.data.name,
                    email:  resp.data.email,
                    mobile_no:  resp.data.mobile_no,
                    points:  resp.data.points,
                    birthdate:  resp.data.birthdate,
                    gender: resp.data.gender,
                    registration_date:  resp.data.registration_date
                }
                $scope.activeVouchers = JSON.parse(resp.data.activeVouchers);
                $rootScope.memberId = resp.data.id;
            }
        });
    } else {
        homeService.loginMember($stateParams.mobileNumber, function(resp) {
            if (resp.message != undefined) {
                $scope.message = 'Customer not found';
            } else {
                $scope.member = {
                    id: resp.data.id,
                    name:  resp.data.name,
                    email:  resp.data.email,
                    mobile_no:  resp.data.mobile_no,
                    points:  resp.data.points,
                    birthdate:  resp.data.birthdate,
                    gender: resp.data.gender,
                    registration_date:  resp.data.registration_date
                }
                $scope.activeVouchers = JSON.parse(resp.data.activeVouchers);
                $rootScope.memberId = resp.data.id;
            }
        });
    }
});
