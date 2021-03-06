package com.jalmansa.meepchallenge.service.vehiclesupdater.impl;

import static java.util.Objects.isNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.jalmansa.meepchallenge.domain.VehicleResource;
import com.jalmansa.meepchallenge.domain.Vehicles;
import com.jalmansa.meepchallenge.domain.VehiclesDifference;
import com.jalmansa.meepchallenge.exception.UnexpectedApiResponseException;
import com.jalmansa.meepchallenge.repository.VehiclesRepository;
import com.jalmansa.meepchallenge.service.apiconsumer.ApiConsumer;
import com.jalmansa.meepchallenge.service.vehiclesupdater.CityVehiclesUpdaterService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CityVehiclesUpdaterServiceImpl implements CityVehiclesUpdaterService {

    private ApiConsumer apiConsumer;
    private VehiclesRepository repo;

    private Vehicles currentVehicles;

    @Autowired
    public CityVehiclesUpdaterServiceImpl(ApiConsumer apiConsumer, VehiclesRepository repo) {
        this.apiConsumer = apiConsumer;
        this.repo = repo;
        currentVehicles = repo.loadCurrent();
        log.info("Succesfully loaded last set of vehicles from 'vehicles.json' file");
    }

    @Override
    public VehiclesDifference execute() throws UnexpectedApiResponseException {
        VehiclesDifference difference = null;

        Vehicles vehiclesResponse = apiConsumer.execute();

        if (isNull(currentVehicles)) {
            log.info("Initializing set of vehicles");

        } else if (vehiclesResponse.equals(currentVehicles)) {
            log.info("SAME SET of available vehicles");

        } else {
            log.info("A NEW SET of vehicles is available");

            SetView<VehicleResource> oldResources = Sets.difference(currentVehicles.getVehicles(), vehiclesResponse.getVehicles());
            SetView<VehicleResource> newResources = Sets.difference(vehiclesResponse.getVehicles(), currentVehicles.getVehicles());

            Vehicles noLongAvailable = Vehicles.of(oldResources);
            Vehicles newVehicles = Vehicles.of(newResources);

            difference = VehiclesDifference.builder()
                    .newAvailable(newVehicles)
                    .noLongerAvailable(noLongAvailable)
                    .build();

            log.info("No longer available vehicles: " + noLongAvailable.toString());
            log.info("New available vehicles: " + newVehicles.toString());

        }
        currentVehicles = vehiclesResponse;
        repo.save(currentVehicles);

        return difference;
    }

}
